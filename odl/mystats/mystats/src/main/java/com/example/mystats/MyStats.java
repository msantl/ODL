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

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;

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

import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyStats{
    public final String sourceIP = "10.0.0.1";
    public final String destinationIP = "10.0.0.2";

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

    private static final Logger log = LoggerFactory.getLogger(MyStats.class);
    private IStatsCollector statsCollector;

    public MyStats() {
    }

    void init() {
        log.debug("INIT called!");
    }

    void destroy() {
        log.debug("DESTROY called!");
    }

    void start() {
        log.debug("START called!");
        getFlowStatistics();
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

    void installFlow(IForwardingRulesManager rulesManger,
            Node node,
            NodeConnector nodeConnector) {

        System.out.print("Installing new flow on: " + node.toString() + " " +
                nodeConnector.getNodeConnectorIdAsString());

        List<Action> actions = new ArrayList<Action>();
        actions.add(new Output(nodeConnector));

        Match match = new Match();

        /* match IPv4 ethernet packets with matching IP addresses*/
        try {
            match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());
            match.setField(MatchType.NW_DST, InetAddress.getByName(destinationIP));
            match.setField(MatchType.NW_SRC, InetAddress.getByName(sourceIP));
        } catch (Exception e) {
            System.out.println("Exception: " + e.toString());
        }

        Flow flow = new Flow(match, actions);
        flow.setPriority(PRIORITY);
        FlowEntry flowEntry = new FlowEntry("GroupName", "FlowName", flow, node);

        Status s = rulesManger.installFlowEntry(flowEntry);

        if (s.isSuccess()) {
            System.out.println(" ... ok");
        } else {
            System.out.println(" ... failed");
        }

        return;
    }

    void getFlowStatistics() {
        String containerName = "default";
        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
            .getInstance(ITopologyManager.class, containerName, this);

        IStatsCollector statsCollector = (IStatsCollector) ServiceHelper
            .getInstance(IStatsCollector.class, containerName, this);

        IForwardingRulesManager rulesManger = (IForwardingRulesManager) ServiceHelper
            .getInstance(IForwardingRulesManager.class, containerName, this);

        Map<Node, List<Data> > edge = null;

        if (statsCollector != null) {
            edge = statsCollector.getLatestStats();
            if (edge == null) {
                System.out.println("StatsCollector returned null!");
                return;
            }
        } else {
            System.out.println("StatsCollector not present!");
            return;
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

        Node startingNode = null, endingNode = null;

        for (Node key : leafs.keySet()) {
            if (leafs.get(key) == null) continue;

            for (Host host : leafs.get(key)) {
                if (host.getNetworkAddressAsString().equals(sourceIP)) {
                    startingNode = key;
                }

                if (host.getNetworkAddressAsString().equals(destinationIP)) {
                    endingNode = key;
                }
            }
        }

        if (startingNode == null || endingNode == null) {
            System.out.println("Couldn't find nodes associated to hosts!");
            return;
        } else {
            System.out.println("start = " + startingNode.toString() +
                               ", end = " + endingNode.toString());
        }

        // dijkstra
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
                long oldPath = Long.MAX_VALUE;
                long newPath = u.dist + 1L;

                if (distance.get(d.getNode()) != null) {
                    oldPath = distance.get(d.getNode());
                }

                if (newPath < oldPath) {
                    v = new Vertex(d.getNode(), newPath);
                    v.prev = u;
                    distance.put(d.getNode(), newPath);

                    q.add(v);
                }
            }
        }

        /* TODO install a new flow */
        if (u != null) {
            u.printPath();  System.out.println("");

            /* delete all flows on nodes */
            v = u;
            while (v != null) {
                List<FlowEntry> flows = rulesManger
                    .getFlowEntriesForNode(v.node);

                if (flows != null) {
                    for (FlowEntry flow : flows) {
                        System.out.print("Uninstalling flow");

                        Status s = rulesManger.uninstallFlowEntry(flow);

                        if (s.isSuccess()) {
                            System.out.println(" ... ok");
                        } else {
                            System.out.println(" ... failed");
                        }
                    }
                }

                v = v.prev;
            }

            /* handle last switch separately */
            Node node = u.node;
            NodeConnector nodeConnector = null;

            for (Data d : edge.get(node)) {
                if (d.getHosts() == null) continue;
                for (Host h : d.getHosts()) {
                    if (destinationIP.equals(h.getNetworkAddressAsString())) {
                        nodeConnector = d.getNodeConnector();
                    }
                }
            }

            if (nodeConnector != null) {
                installFlow(rulesManger, node, nodeConnector);
            }

            while (u.prev != null) {
                node = u.node;
                u = u.prev;

                Node key = u.node;
                nodeConnector = null;

                for (Data d : edge.get(key)) {
                    if (d.getNode() == null) continue;
                    if (d.getNode().equals(node)) {
                        nodeConnector = d.getNodeConnector();
                    }
                }

                if (nodeConnector == null) {
                    System.out.println("An error occured while searching for node connector!");
                    return;
                }

                installFlow(rulesManger, key, nodeConnector);
            }

        } else {
            System.out.println("Couldn't find a path!");
            return;
        }



        return;
    }
}
