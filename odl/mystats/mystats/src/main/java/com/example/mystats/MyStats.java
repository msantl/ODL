package com.example.mystats;

import java.net.InetAddress;

import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.NavigableSet;
import java.util.TreeSet;

import com.example.statscollector.IStatsCollector;
import com.example.statscollector.Data;

import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.NetUtils;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.topologymanager.ITopologyManager;

import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;

import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.Ethernet;

import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyStats implements IListenDataPacket {
    public final String containerName = "default";
    public final short PRIORITY = 1;

    public static class Vertex implements Comparable<Vertex> {
        public final Node node;
        public long dist = Long.MAX_VALUE;
        public Vertex prev = null;

        public Vertex(Node node) {
            this.node = node;
        }

        public Vertex(Node node, long dist) {
            this.node = node;
            this.dist = dist;
        }

        private void printPath() {
            if (this == this.prev) {
                System.out.print(node.toString());
            } else if (this.prev == null) {
                System.out.print(node.toString());
            } else {
                this.prev.printPath();
                System.out.print(" -> " + node.toString() + "[" + this.dist + "]");
            }
        }

        public int compareTo(Vertex other) {
            if (dist < other.dist) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public static class Path {
        public final Node n1;
        public final Node n2;
        public final NodeConnector n1n2;
        public final NodeConnector n2n1;

        public Path(Node in, Node out, NodeConnector in2out, NodeConnector out2in) {
            this.n1 = in;
            this.n2 = out;
            this.n1n2 = in2out;
            this.n2n1 = out2in;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(MyStats.class);

    public MyStats() {
    }

    void init() {
        log.debug("INIT called!");
        clearAllFlows();
    }

    void destroy() {
        log.debug("DESTROY called!");
    }

    void start() {
        log.debug("START called!");
    }

    void stop() {
        log.debug("STOP called!");
    }

    void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        System.out.println("--------------------");
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            try {
                System.out.println(pairs.getKey() + " = " + pairs.getValue());
            } catch (Exception e) {
                System.out.println("n/a");
            }
        }
        System.out.println("--------------------");
    }

    void clearAllFlows() {
        System.out.println("Removing all flows for all nodes!");

        IForwardingRulesManager rulesManger = (IForwardingRulesManager) ServiceHelper
            .getInstance(IForwardingRulesManager.class, containerName, this);

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
            .getInstance(ISwitchManager.class, containerName, this);

        for (Switch swc: switchManager.getNetworkDevices()) {
            Node node = swc.getNode();

            List<FlowEntry> flows = rulesManger.getFlowEntriesForNode(node);

            if (flows != null) {
                for (FlowEntry flow: flows) {
                    System.out.print("Uninstalling flow for " + node.toString());

                    Status s = rulesManger.uninstallFlowEntry(flow);

                    if (s.isSuccess()) {
                        System.out.println(" ... ok");
                    } else {
                        System.out.println(" ... failed");
                    }
                }
            }
        }

        return;
    }

    void installFlow(IForwardingRulesManager rulesManger,
            Node node,
            NodeConnector nodeConnector,
            InetAddress src,
            InetAddress dst) {

        System.out.print("Installing new flow on: " + node.toString() + " " +
                nodeConnector.getNodeConnectorIdAsString());

        List<Action> actions = new ArrayList<Action>();
        actions.add(new Output(nodeConnector));

        Match match = new Match();

        /* match IPv4 ethernet packets with matching IP addresses*/
        try {
            match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());
            match.setField(MatchType.NW_DST, dst);
            match.setField(MatchType.NW_SRC, src);
        } catch (Exception e) {
            System.out.println("Exception: " + e.toString());
        }

        Flow flow = new Flow(match, actions);
        flow.setPriority(PRIORITY);

        String policyName = src.getHostAddress() + "/" + dst.getHostAddress();
        String flowName = "[" + node.toString() + "] " + policyName;

        FlowEntry flowEntry = new FlowEntry(policyName, "MyStatsFlow", flow, node);

        Status s = rulesManger.installFlowEntry(flowEntry);

        if (s.isSuccess()) {
            System.out.println(" ... ok");
        } else {
            System.out.println(" ... failed");
        }

        return;
    }

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
            .getInstance(ITopologyManager.class, containerName, this);

        IStatsCollector statsCollector = (IStatsCollector) ServiceHelper
            .getInstance(IStatsCollector.class, containerName, this);

        IForwardingRulesManager rulesManger = (IForwardingRulesManager) ServiceHelper
            .getInstance(IForwardingRulesManager.class, containerName, this);

        IDataPacketService dataPacket = (IDataPacketService) ServiceHelper
            .getInstance(IDataPacketService.class, containerName, this);

        System.out.println("Received new packet!");

        /* extract IP addresses from packet data */
        Packet formattedPak = dataPacket.decodeDataPacket(inPkt);
        InetAddress source = null, destination = null;

        if (formattedPak instanceof Ethernet) {
            Object nextPak = formattedPak.getPayload();
            if (nextPak instanceof IPv4) {
                IPv4 ipPak = (IPv4)nextPak;
                source = NetUtils.getInetAddress(ipPak.getSourceAddress());
                destination = NetUtils.getInetAddress(ipPak.getDestinationAddress());
            }
        }

        if (source == null || destination == null) {
            System.out.println("Packet doesn't have src or dst IP address!");
            return PacketResult.IGNORED;
        }

        Map<Node, List<Data> > edge = null;

        if (statsCollector != null) {
            edge = statsCollector.getLatestStats();
            if (edge == null) {
                System.out.println("StatsCollector returned null!");
                return PacketResult.IGNORED;
            }
        } else {
            System.out.println("StatsCollector not present!");
            return PacketResult.IGNORED;
        }


        // get topology information
        Map<Node, Set<Edge>> topology = topologyManager.getNodeEdges();
        Map<Node, List<Host>> leafs = new HashMap<Node, List<Host>>();

        // update neighbour information
        for (Node key : edge.keySet()) {
            // for each node we get from stats collector

            for (Edge e : topology.get(key)) {
                // get topology information about that node

                NodeConnector tail_nc = e.getTailNodeConnector();
                NodeConnector head_nc = e.getHeadNodeConnector();

                Node tail = tail_nc.getNode();
                Node head = head_nc.getNode();

                // save vertex(head -> tail)
                List<Data> neighbours = edge.get(head);
                for (Data n : neighbours) {
                    if (head_nc.equals(n.getNodeConnector())) {
                        n.setNode(tail);
                    }
                }
            }
        }

        // update host info
        for (Node key : edge.keySet()) {
            // for each node we get from stats collector
            for (Data n : edge.get(key)) {
                // get topology information about hosts connected to switch
                List<Host> hosts = topologyManager
                    .getHostsAttachedToNodeConnector(n.getNodeConnector());

                if (hosts != null) {
                    n.setHosts(hosts);

                    // update the leafs map
                    List<Host> currentHosts = new ArrayList<Host>(hosts);
                    if (leafs.get(key) != null) {
                        currentHosts.addAll(leafs.get(key));
                    }

                    leafs.put(key, currentHosts);
                }
            }
        }

        // print collected information
        for (Node key : edge.keySet()) {
            System.out.println("Switch: " + key.toString());

            for (Data data : edge.get(key)) {
                if (data.getNode() != null) {
                    System.out.println("\tNode: " + data.getNode().toString());
                }
                if (data.getHosts() != null) {
                    System.out.print("\tHosts: ");
                    for (Host h : data.getHosts()) {
                        System.out.print(h.getNetworkAddressAsString() + ", ");
                    }
                    System.out.println("");
                }

                System.out.println("\tNodeConnector: " +
                        data.getNodeConnector().getNodeConnectorIdAsString());
                System.out.println("\tPacketDrop: " + data.getPacketDrop());
                System.out.println("\tPacketSent: " + data.getPacketSent());

                System.out.println("\tTx count: " + data.getTxCount());
                System.out.println("\tRx count: " + data.getRxCount());
                System.out.println("\tBandwidth: " + data.getBandwidth());
                System.out.println("");
            }
        }

        /* run dijkstra on collected data */
        System.out.println("Computing the shortest path between h1 and h2");

        /* find switches that are connected to hosts */
        Node startingNode = null, endingNode = null;
        for (Node key : leafs.keySet()) {
            if (leafs.get(key) == null) continue;

            for (Host host : leafs.get(key)) {
                if (host.getNetworkAddress().equals(source)) {
                    startingNode = key;
                }

                if (host.getNetworkAddress().equals(destination)) {
                    endingNode = key;
                }
            }
        }

        if (startingNode == null || endingNode == null) {
            System.out.println("Couldn't find nodes associated to hosts!");
            return PacketResult.IGNORED;
        } else {
            System.out.println("start = " + startingNode.toString() +
                               ", end = " + endingNode.toString());
        }

        /* dijkstra */
        NavigableSet<Vertex> q = new TreeSet<Vertex>();
        Map<Node, Long> distance = new HashMap<Node, Long>();

        q.add(new Vertex(startingNode, 0L));
        distance.put(startingNode, 0L);

        Vertex u = null, v;

        while (!q.isEmpty()) {
            u = q.pollFirst();

            if (u.node.equals(endingNode)) {
                System.out.println("Path found!");
                break;
            }

            for (Data d : edge.get(u.node)) {
                long oldPathCost = Long.MAX_VALUE;
                long newPathCost = u.dist + 1L;

                if (distance.get(d.getNode()) != null) {
                    oldPathCost = distance.get(d.getNode());
                }

                if (newPathCost < oldPathCost) {
                    v = new Vertex(d.getNode(), newPathCost);
                    v.prev = u;
                    distance.put(d.getNode(), newPathCost);

                    q.add(v);
                }
            }
        }

        /* install new flows */
        if (u == null) {
            System.out.println("Couldn't find a path!");
            return PacketResult.IGNORED;
        }

        List<Path> path = new ArrayList<Path>();

        while (u.prev != null) {
            Node n1 = u.node;
            Node n2 = u.prev.node;

            NodeConnector n1n2 = null, n2n1 = null;

            for (Data d: edge.get(n1)) {
                if (d.getNodeConnector() != null &&
                    d.getNode() != null &&
                    n2.equals(d.getNode())) {

                    n1n2 = d.getNodeConnector();
                    break;
                }
            }

            for (Data d: edge.get(n2)) {
                if (d.getNodeConnector() != null &&
                    d.getNode() != null &&
                    n1.equals(d.getNode())) {

                    n2n1 = d.getNodeConnector();
                    break;
                }
            }

            if (n1n2 == null || n2n1 == null) {
                System.out.println("NodeConnector not found!");
                continue;
            }

            path.add(new Path(n1, n2, n1n2, n2n1));

            u = u.prev;
        }

        /* path is now a list of pairs (n1, n2) -> (n2, n3) -> ... */

        for (Path p: path) {
            List<FlowEntry> flows;

            /* delete existing flow for n1 */
            flows = rulesManger.getFlowEntriesForNode(p.n1);
            if (flows != null) {
                for (FlowEntry flow: flows) {
                    System.out.print("Uninstalling flow for " + p.n1.toString());

                    Status s = rulesManger.uninstallFlowEntry(flow);

                    if (s.isSuccess()) {
                        System.out.println(" ... ok");
                    } else {
                        System.out.println(" ... failed");
                    }
                }
            }

            /* delete existing flow for n2 */
            flows = rulesManger.getFlowEntriesForNode(p.n2);
            if (flows != null) {
                for (FlowEntry flow: flows) {
                    System.out.print("Uninstalling flow for " + p.n2.toString());

                    Status s = rulesManger.uninstallFlowEntry(flow);

                    if (s.isSuccess()) {
                        System.out.println(" ... ok");
                    } else {
                        System.out.println(" ... failed");
                    }
                }
            }
        }

        for (Path p: path) {
            /* install new flow for n1-n2 and n2-n1 */
            installFlow(rulesManger, p.n1, p.n1n2, destination, source);
            installFlow(rulesManger, p.n2, p.n2n1, source, destination);

            /* check if n2 is source */
            if (p.n2.equals(startingNode)) {
                NodeConnector n2h = null;

                for (Data d: edge.get(p.n2)) {
                    if (d.getHosts() != null) {
                        for (Host h: d.getHosts()) {
                            if (source.equals(h.getNetworkAddress())) {
                                n2h = d.getNodeConnector();
                            }
                        }
                    }
                }

                if (n2h != null) {
                    installFlow(rulesManger, p.n2, n2h, destination, source);
                }
            }

            /* check if n1 is destination */
            if (p.n1.equals(endingNode)) {
                NodeConnector n1h = null;

                for (Data d: edge.get(p.n1)) {
                    if (d.getHosts() != null) {
                        for (Host h: d.getHosts()) {
                            if (destination.equals(h.getNetworkAddress())) {
                                n1h = d.getNodeConnector();
                            }
                        }
                    }
                }

                if (n1h != null) {
                    installFlow(rulesManger, p.n1, n1h, source, destination);
                }
            }
        }

       return PacketResult.CONSUME;
    }
}
