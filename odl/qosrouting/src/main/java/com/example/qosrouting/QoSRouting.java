package com.example.qosrouting;

import java.net.InetAddress;

import java.util.List;
import java.util.ArrayList;

import java.util.Set;

import java.util.Map;

import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;

import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.NetUtils;

// import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
// import org.opendaylight.controller.sal.utils.ServiceHelper;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
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

public class QoSRouting implements IListenDataPacket {
    public final String containerName = "default";
    public final short PRIORITY = 1;

    private IForwardingRulesManager forwardRulesManager;
    private IDataPacketService dataPacketService;
    private ITopologyManager topologyManager;
    private ISwitchManager switchManager;
    private IfIptoHost hostManager;

    private static final Logger log = LoggerFactory.getLogger(QoSRouting.class);

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

    public QoSRouting() {
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

    void installFlow(Node node, NodeConnector nodeConnector,
                     InetAddress src, InetAddress dst) {

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

        FlowEntry flowEntry = new FlowEntry(policyName, flowName, flow, node);

        Status s = this.forwardRulesManager.installFlowEntry(flowEntry);

        if (s.isSuccess()) {
            System.out.println(" ... ok");
        } else {
            System.out.println(" ... failed");
        }

        return;
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

        /* wait until we know where the hosts are */
        System.out.print("Waiting for source to appear in known hosts");
        HostNodeConnector srcHost = null;
        do {
            srcHost = hostManager.hostFind(source);

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
            dstHost = hostManager.hostFind(destination);

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

        Map<Node, Set<Edge> > edges = this.topologyManager.getNodeEdges();
        /*
        IStatisticsManager statsManager = (IStatisticsManager) ServiceHelper
            .getInstance(IStatisticsManager.class, containerName, this);
        */

        /* compute the shortest path */
        List<Edge> path = Dijkstra.getPathHopByHop(srcNode, dstNode, edges);
            //,statsManager);

        /* set flow for source */
        installFlow(srcNode, srcNodeConnector, destination, source);

        /* set flow for destination */
        installFlow(dstNode, dstNodeConnector, source, destination);

        /* install flows along the path */
        for (Edge e: path) {
            installFlow(e.getHeadNodeConnector().getNode(),
                    e.getHeadNodeConnector(), source, destination);

            installFlow(e.getTailNodeConnector().getNode(),
                    e.getTailNodeConnector(), destination, source);
        }

        /* send the first packet manually */
        System.out.println("Sending packet to: " + dstNodeConnector.toString());

        RawPacket rp = this.dataPacketService
            .encodeDataPacket(ipPak.getParent());

        rp.setOutgoingNodeConnector(dstNodeConnector);
        this.dataPacketService.transmitDataPacket(rp);

        return PacketResult.CONSUME;
    }
}
