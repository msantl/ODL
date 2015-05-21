package com.example.mjerenje1;

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
import org.opendaylight.controller.sal.packet.Ethernet;

import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mjerenje1 implements IListenDataPacket {
    public final String     containerName = "default";
    public final String     PROPNAME = "bandwidth";
    public final long       MTU = 1500 * 8;
    public final short      PRIORITY = 1;
    public final short      IDLE_TIMEOUT = 5;

    private IForwardingRulesManager forwardRulesManager;
    private IDataPacketService dataPacketService;
    private ITopologyManager topologyManager;
    private ISwitchManager switchManager;
    private IStatisticsManager statisticsManager;
    private IfIptoHost hostManager;

    private static final Logger log = LoggerFactory.getLogger(Mjerenje1.class);

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

    public Mjerenje1() {
    }

    void init() {
        log.debug("INIT called!");

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
    }

    void clearAllFlows() {
        System.out.println("Removing all flows for all nodes!");

        for (Switch swc: this.switchManager.getNetworkDevices()) {
            Node node = swc.getNode();

            List<FlowEntry> flows = this.forwardRulesManager
                .getFlowEntriesForNode(node);

            if (flows != null) {
                for (FlowEntry flow: flows) {
                    // System.out.print("Uninstalling flow on " + node.toString());

                    Status s = this.forwardRulesManager
                        .uninstallFlowEntry(flow);

                    if (s.isSuccess()) {
                        // System.out.println(" ... ok");
                    } else {
                        // System.out.println(" ... failed");
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
            //System.out.println("Create match exception: " + e.toString());
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
            /*
            System.out.println("Setting Controller to listen for packets on " +
                    node.toString() + " to " + dst.getHostAddress());*/

            actions.add(new Controller());
        }

        Match match = createMatch(src, dst);
        Flow flow = new Flow(match, actions);
        flow.setPriority(PRIORITY);
        flow.setIdleTimeout(IDLE_TIMEOUT);

        String policyName = src.getHostAddress() + "/" + dst.getHostAddress();
        String flowName = "[" + node.toString() + "] " + policyName;

        FlowEntry flowEntry = new FlowEntry(policyName, flowName, flow, node);

        /*System.out.print("Installing new flow on: " + node.toString() + " " +
                nodeConnector.getNodeConnectorIdAsString());*/

        Status s = this.forwardRulesManager.installFlowEntry(flowEntry);

        if (s.isSuccess()) {
            // System.out.println(" ... ok");
        } else {
            // System.out.println(" ... failed");
        }

        return;
    }

    void createNewFlow(InetAddress source, InetAddress destination,
            IPv4 ipPak, TrafficType trafficType, boolean listen) {
        /* wait until we know where the hosts are */
        //System.out.print("Waiting for source to appear in known hosts");
        HostNodeConnector srcHost = null;
        do {
            srcHost = this.hostManager.hostFind(source);

            try{
                Thread.sleep(100);
            } catch(Exception e) {
                //System.out.println(e.toString());

            }
            //System.out.print(".");

        } while(srcHost == null);
        //System.out.println("done");

        //System.out.print("Waiting for destination to appear in known hosts");
        HostNodeConnector dstHost = null;
        do {
            dstHost = this.hostManager.hostFind(destination);

            try{
                Thread.sleep(100);
            } catch(Exception e) {
                //System.out.println(e.toString());
            }

            //System.out.print(".");

        } while(dstHost == null);
        //System.out.println("done");

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
            System.out.println("Src: " + source.getHostAddress() +
                    " Dst: " + destination.getHostAddress());

            Map<Node, Set<Edge> > edges = this.topologyManager.getNodeEdges();

            /* compute the shortest path according to type */
            List<Edge> path;

            if (trafficType == TrafficType.OTHER) {
                path = Dijkstra.getPathHopByHop(srcNode, dstNode, edges);
            } else {
                Map<Node, Double> packetLoss = this.getPacketLossEstimations();
                Map<Node, Double> delay = this.getDelayEstimations();

                AntColony aco = new AntColony(edges, srcNode, dstNode,
                        trafficType, packetLoss, delay);

                aco.run();

                path = aco.getPath();
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
                /*System.out.println("Sending packet to: " +
                        dstNodeConnector.toString());*/

                RawPacket rp = this.dataPacketService
                    .encodeDataPacket(ipPak.getParent());

                rp.setOutgoingNodeConnector(dstNodeConnector);
                this.dataPacketService.transmitDataPacket(rp);
            }

        } else {
            //System.out.println("Flow for matching IP addresses found!");
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

        //System.out.println("Received new packet!");

        /* create new a flow between source and destination */
        createNewFlow(source, destination, ipPak, TrafficType.OTHER, true);

        return PacketResult.CONSUME;
    }
}
