package com.example.samcra;

import java.net.InetAddress;

import java.util.List;
import java.util.ArrayList;

import java.util.Set;

import java.util.Map;
import java.util.HashMap;

import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;

import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.NetUtils;

import org.opendaylight.controller.statisticsmanager.IStatisticsManager;

import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Bandwidth;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;

import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.TCP;
import org.opendaylight.controller.sal.packet.UDP;
import org.opendaylight.controller.sal.packet.Ethernet;

import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Samcra implements IListenDataPacket {
    public final String     containerName = "default";
    public final String     PROPNAME = "bandwidth";
    public final long       MTU = 1500 * 8;
    public final short      PRIORITY = 1;
    public final short      IDLE_TIMEOUT = 5;
    public final int        SAMCRA = 0;
    public final double     BANDWIDTH = Double.MAX_VALUE;

    private IForwardingRulesManager forwardRulesManager;
    private IDataPacketService dataPacketService;
    private ITopologyManager topologyManager;
    private ISwitchManager switchManager;
    private IStatisticsManager statisticsManager;
    private IfIptoHost hostManager;

    private static final Logger log = LoggerFactory.getLogger(Samcra.class);

    public void setStatisticsManager(IStatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
    }

    public void unsetStatisticsManager(IStatisticsManager statisticsManager) {
        if (this.statisticsManager == statisticsManager) {
            this.statisticsManager = null;
        }
    }

    public void setSwitchManager(ISwitchManager switchManager) {
        this.switchManager = switchManager;
    }

    public void unsetSwitchManager(ISwitchManager switchManager) {
        if (this.switchManager == switchManager) {
            this.switchManager = null;
        }
    }

    public void setTopologyManager(ITopologyManager topologyManager) {
        this.topologyManager = topologyManager;
    }

    public void unsetTopologyManager(ITopologyManager topologyManager) {
        if (this.topologyManager == topologyManager) {
            this.topologyManager = null;
        }
    }

    public void setForwardingRulesManager(
            IForwardingRulesManager forwardRulesManager) {
        this.forwardRulesManager = forwardRulesManager;
    }

    public void unsetForwardingRulesManager(
            IForwardingRulesManager forwardRulesManager) {
        if (this.forwardRulesManager == forwardRulesManager) {
            this.forwardRulesManager = null;
        }
    }

    public void setDataPacketService(IDataPacketService dataPacketService) {
        this.dataPacketService = dataPacketService;
    }

    public void unsetDataPacketService(IDataPacketService dataPacketService) {
        if (this.dataPacketService == dataPacketService) {
            this.dataPacketService = null;
        }
    }

    public void setIfIptoHost(IfIptoHost hostManager) {
        this.hostManager = hostManager;
    }

    public void unsetIfIptoHost(IfIptoHost hostManager) {
        if (this.hostManager == hostManager) {
            this.hostManager = null;
        }
    }

    public Samcra() {
    }

    void init() {
        log.debug("INIT called!");

        /* load SAMCRA library */
        System.loadLibrary("XAMCRA");
        /* remove all existing flows*/
        clearAllFlows();
    }

    void destroy() {
        log.debug("DESTROY called!");
    }

    void start() {
        log.debug("START called!");

        /* check all depdendencies */
        if (this.switchManager == null) {
            System.out.println("SwitchManager not present!");
        }

        if (this.topologyManager == null) {
            System.out.println("TopologyManager not present!");
        }

        if (this.forwardRulesManager == null) {
            System.out.println("ForwardingRulesManager not present!");
        }

        if (this.dataPacketService == null) {
            System.out.println("DataPacketService not present!");
        }

        if (this.hostManager == null) {
            System.out.println("HostManager not present!");
        }

        if (this.switchManager == null) {
            System.out.println("SwitchManager not present!");
        }
    }

    void stop() {
        log.debug("STOP called!");
        JNIXAMCRA.jnikillXamcra();
    }

    void clearAllFlows() {
        System.out.println("Removing all flows for all nodes!");

        for (Switch swc: this.switchManager.getNetworkDevices()) {
            Node node = swc.getNode();

            List<FlowEntry> flows = this.forwardRulesManager
                .getFlowEntriesForNode(node);

            if (flows != null) {
                for (FlowEntry flow: flows) {
                    System.out.print("Uninstalling flow on " + node.toString());

                    Status s = this.forwardRulesManager
                        .uninstallFlowEntry(flow);

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

    Match createMatch(InetAddress src, InetAddress dst) {
        Match match = new Match();

        /* match IPv4 ethernet packets with matching IP addresses*/
        try {
            match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());
            match.setField(MatchType.NW_DST, dst);
            match.setField(MatchType.NW_SRC, src);
        } catch (Exception e) {
            System.out.println("Exception: " + e.toString());
            return null;
        }

        return match;
    }

    void installFlow(Node node, NodeConnector nodeConnector,
                     InetAddress src, InetAddress dst,
                     boolean setContollerToListen) {

        List<Action> actions = new ArrayList<Action>();
        actions.add(new Output(nodeConnector));

        if (setContollerToListen) {
            System.out.println("Setting Controller to listen for packets on " +
                    node.toString() + " to " + dst.getHostAddress());

            actions.add(new Controller());
        }

        Match match = createMatch(src, dst);
        Flow flow = new Flow(match, actions);
        flow.setPriority(PRIORITY);
        flow.setIdleTimeout(IDLE_TIMEOUT);

        String policyName = src.getHostAddress() + "/" + dst.getHostAddress();
        String flowName = "[" + node.toString() + "] " + policyName;

        FlowEntry flowEntry = new FlowEntry(policyName, flowName, flow, node);

        System.out.print("Installing new flow on: " + node.toString() + " " +
                nodeConnector.getNodeConnectorIdAsString());

        Status s = this.forwardRulesManager.installFlowEntry(flowEntry);

        if (s.isSuccess()) {
            System.out.println(" ... ok");
        } else {
            System.out.println(" ... failed");
        }

        return;
    }

    void createNewFlow(InetAddress source, InetAddress destination,
            IPv4 ipPak, boolean listen) {
        /* wait until we know where the hosts are */
        System.out.print("Waiting for source to appear in known hosts");
        HostNodeConnector srcHost = null;
        do {
            srcHost = this.hostManager.hostFind(source);

            try{
                Thread.sleep(100);
            } catch(Exception e) {
                System.out.println(e.toString());

            }
            System.out.print(".");

        } while(srcHost == null);
        System.out.println("done");

        System.out.print("Waiting for destination to appear in known hosts");
        HostNodeConnector dstHost = null;
        do {
            dstHost = this.hostManager.hostFind(destination);

            try{
                Thread.sleep(100);
            } catch(Exception e) {
                System.out.println(e.toString());
            }

            System.out.print(".");

        } while(dstHost == null);
        System.out.println("done");

        /* get the starting point */
        NodeConnector srcNodeConnector = srcHost.getnodeConnector();
        Node srcNode = srcHost.getnodeconnectorNode();

        /* get the ending point */
        NodeConnector dstNodeConnector = dstHost.getnodeConnector();
        Node dstNode = dstHost.getnodeconnectorNode();

        /* check if the flow already exists */
        List<FlowEntry> flows = this.forwardRulesManager
            .getInstalledFlowEntriesForNode(srcNode);

        boolean needToInstallFlow = true;

        if (flows != null) {
            for (FlowEntry flow: flows) {
                Match match = createMatch(source, destination);
                if (match.equals(flow.getFlow().getMatch())) {
                    needToInstallFlow = false;
                    break;
                }
            }
        }

        if (needToInstallFlow) {
            Map<Node, Set<Edge> > edges = this.topologyManager.getNodeEdges();

            /* compute the shortest path according to type */
            List<Edge> path = null;

            int nodeCount = edges.keySet().size();
            int linkCount = 0;
            /* bandwidth, delay, packet loss*/
            int metricsCount = 3;

            /* helper structs */
            Map<Node, Integer> node2int = new HashMap<Node, Integer>();
            Map<Integer, Node> int2node = new HashMap<Integer, Node>();

            int id = 1;
            for (Node node: edges.keySet()) {
                node2int.put(node, id);
                int2node.put(id, node);
                id += 1;
            }


            for (Node node: edges.keySet()) {
                for (Edge edge: edges.get(node)) {
                    if (edge.getHeadNodeConnector().getNode().equals(node)) continue;

                    int i = node2int.get(edge.getTailNodeConnector().getNode());
                    int j = node2int.get(edge.getHeadNodeConnector().getNode());

                    if (i < j) linkCount += 1;
                }
            }

            /* init XAMCRA */
            System.out.println("Initializing XAMCRA: " + nodeCount + ", " + linkCount);
            try {
                JNIXAMCRA.jniinitXamcra(nodeCount, linkCount, metricsCount, SAMCRA);
            } catch(Exception e){
                e.printStackTrace();
                System.out.println("init error " + e.getMessage());
            }

            /* add nodes to XAMCRA */
            for (Node node: edges.keySet()) {
                System.out.println("Adding node: " + node2int.get(node));
                JNIXAMCRA.jniaddNode(node2int.get(node));
            }

            Map<Node, Double> delay = this.getDelayEstimations();
            Map<Node, Double> packet_loss = this.getPacketLossEstimations();

            /* add links to XAMCRA */
            for (Node node: edges.keySet()) {
                for (Edge edge: edges.get(node)) {
                    if (edge.getHeadNodeConnector().getNode().equals(node)) continue;
                    double[] metrics = new double[metricsCount - 1];

                    Node src = edge.getTailNodeConnector().getNode();
                    Node dst = edge.getHeadNodeConnector().getNode();

                    int src_id = node2int.get(src);
                    int dst_id = node2int.get(dst);

                    if (src_id >= dst_id) continue;

                    double d = delay.get(src);
                    double p = -1 * Math.log(1 - packet_loss.get(src));

                    metrics[0] = d; /* delay */
                    metrics[1] = p; /* packet loss*/

                    System.out.println("Adding link: " + src_id + ", " + dst_id);
                    JNIXAMCRA.jniaddLink(src_id, dst_id, BANDWIDTH, metrics);
                }
            }

            int[] nodePath = null;
            double[] metricsContraint = new double[metricsCount - 1];

            metricsContraint[0] = Double.MAX_VALUE;
            metricsContraint[1] = Double.MAX_VALUE;

            System.out.println("Computing path...");
            nodePath = JNIXAMCRA
                .jnicomputePath(node2int.get(srcNode), node2int.get(dstNode), BANDWIDTH, metricsContraint, 0);

            /* Convert path[] into list of edges */
            for (int i = 1; i < nodePath.length; ++i) {
                /* find the edge that goes from edges.get(nodePath[i-1]) to nodePath[i] */
                Node src = int2node.get(nodePath[i-1]);
                Edge e = null;

                for (Edge edge: edges.get(src)) {
                    if (edge.getTailNodeConnector().getNode().equals(src)) {
                        Node tmp = edge.getHeadNodeConnector().getNode();

                        if (node2int.get(tmp) == nodePath[i]) {
                            e = edge;
                            break;
                        }
                    }
                }

                if (e == null) {
                    System.out.println("Couldn't find a path!");
                    return;
                }

                /* insert that edge into list path */
                path.add(e);
            }

            /* set flow for source */
            installFlow(srcNode, srcNodeConnector, destination, source, listen);

            /* set flow for destination */
            installFlow(dstNode, dstNodeConnector, source, destination, listen);

            /* install flows along the path */
            for (Edge e: path) {
                installFlow(e.getHeadNodeConnector().getNode(),
                        e.getHeadNodeConnector(), source, destination,
                        false);

                installFlow(e.getTailNodeConnector().getNode(),
                        e.getTailNodeConnector(), destination, source,
                        false);
            }

            /* send the first packet manually */
            if (ipPak != null) {
                System.out.println("Sending packet to: " +
                        dstNodeConnector.toString());

                RawPacket rp = this.dataPacketService
                    .encodeDataPacket(ipPak.getParent());

                rp.setOutgoingNodeConnector(dstNodeConnector);
                this.dataPacketService.transmitDataPacket(rp);
            }

        } else {
            System.out.println("Flow for matching IP addresses found!");
        }

        return;
    }

    InetAddress parsePacketData(String data) {
        InetAddress ret = null;

        while (ret == null && data.length() > 0) {
            try {
                ret = InetAddress.getByName(data);
            } catch(Exception e) {
                data = data.substring(0, data.length() - 1);
            }
        }

        return ret;
    }

    public Map<Node, Double> getPacketLossEstimations() {
        Map<Node, Double> ret = new HashMap<Node, Double>();

        for (Switch swc: this.switchManager.getNetworkDevices()) {
            Node node = swc.getNode();
            double loss = 1.0;

            for (NodeConnector nc: this.switchManager.getUpNodeConnectors(node)) {
                NodeConnectorStatistics stat = this.statisticsManager
                    .getNodeConnectorStatistics(nc);

                loss *= 1 - ((stat.getTransmitDropCount() +
                              stat.getReceiveDropCount()) /
                             (stat.getTransmitPacketCount() +
                              stat.getReceivePacketCount()));
            }

            ret.put(node, 1 - loss);
        }

        return ret;
    }

    public Map<Node, Double> getDelayEstimations() {
        Map<Node, Double> ret = new HashMap<Node, Double>();

        for (Switch swc: this.switchManager.getNetworkDevices()) {
            Node node = swc.getNode();
            Double delay = 0.0;

            /* get node average bandwidth */
            Double bandwidth = 0.0;
            for (NodeConnector nc: swc.getNodeConnectors()) {
                /* delay is given in bps */
                Bandwidth prop = (Bandwidth) this.switchManager
                    .getNodeConnectorProp(nc, PROPNAME);

                bandwidth += prop.getValue();
            }
            bandwidth /= swc.getNodeConnectors().size();

            /* get flow table entries for node */
            int flowTableEntries = this.statisticsManager
                .getFlowsNumber(node);

            delay = flowTableEntries / 2.0 * (MTU / bandwidth);

            if (delay.equals(Double.NaN)) {
                ret.put(node, Double.MAX_VALUE);
            } else {
                ret.put(node, delay);
            }
        }
        return ret;
    }

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        /* extract IP addresses from packet data */
        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        InetAddress source = null, destination = null;
        IPv4 ipPak = null;

        if (formattedPak instanceof Ethernet) {
            Object nextPak = formattedPak.getPayload();
            if (nextPak instanceof IPv4) {
                ipPak = (IPv4)nextPak;

                source = NetUtils
                    .getInetAddress(ipPak.getSourceAddress());
                destination = NetUtils
                    .getInetAddress(ipPak.getDestinationAddress());
            }
        }

        if (source == null || destination == null) {
            return PacketResult.IGNORED;
        }

        System.out.println("Received new packet!");
        System.out.println("Src: " + source.getHostAddress() +
                          " Dst: " + destination.getHostAddress());


        /* create new a flow between source and destination */
        createNewFlow(source, destination, ipPak, true);

        /* Check the packet content */
        try {
            byte[] payload = null;
            Object pak = formattedPak;

            if (pak instanceof Ethernet) {
                pak = ((Ethernet) pak).getPayload();
                if (pak instanceof IPv4) {
                    pak = ((IPv4) pak).getPayload();

                    if (pak instanceof TCP) {
                        payload = ((TCP) pak).getRawPayload();
                    }
                    if (pak instanceof UDP) {
                        payload = ((UDP) pak).getRawPayload();
                    }
                }
            }

            /* parse UDP/TCP packet payload */
            if (payload != null) {
                String data = new String(payload, "UTF-8");

                InetAddress streamAddress = parsePacketData(data);

                if (streamAddress != null) {
                    System.out.println("IP: " + streamAddress.getHostAddress());

                    /* create a new flow for video stream between destination
                     * and stream address */
                    createNewFlow(destination, streamAddress, null, false);
                }
            }

        } catch(Exception e) {
            System.out.println(e.toString());
        }

        return PacketResult.CONSUME;
    }
}
