package com.example.mystats;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyStats{
    private static final Logger log = LoggerFactory.getLogger(MyStats.class);

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

    public class Data {
        private Node node;
        private List<Host> host;
        private NodeConnector nc;
        private long pkt_drop;
        private long pkt_sent;
        private String bandwidth;

        public Data() {}

        public Data(Node node,
                    NodeConnector nc,
                    long pkt_drop,
                    long pkt_sent,
                    String bandwidth) {
            this.node = node;
            this.nc = nc;
            this.pkt_drop = pkt_drop;
            this.pkt_sent = pkt_sent;
            this.bandwidth = bandwidth;
        }

        public void setNode(Node node) {this.node = node;}
        public void setNodeConnector(NodeConnector nc) {this.nc = nc;}
        public void setPacketDrop(long pkt_drop) {this.pkt_drop = pkt_drop;}
        public void setPacketSent(long pkt_sent) {this.pkt_sent = pkt_sent;}
        public void setBandwidth(String bandwidth) {this.bandwidth = bandwidth;}
        public void setHosts(List<Host> hosts) {
            this.host = new ArrayList();
            for (Host h : hosts) {
                this.host.add(h);
            }
        }

        public Node getNode() {return this.node;}
        public NodeConnector getNodeConnector() {return this.nc;}
        public long getPacketDrop() {return this.pkt_drop;}
        public long getPacketSent() {return this.pkt_sent;}
        public String getBandwidth() {return this.bandwidth;}
        public List<Host> getHosts() {return this.host;}
    }

    void getFlowStatistics() {
        String containerName = "default";
        String propertyName = "bandwidth";

        Map<Node, List<Data> > edge = new HashMap();

        IStatisticsManager statsManager = (IStatisticsManager) ServiceHelper
            .getInstance(IStatisticsManager.class, containerName, this);

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
            .getInstance(ISwitchManager.class, containerName, this);

        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
            .getInstance(ITopologyManager.class, containerName, this);

        List<Switch> switches =
            switchManager.getNetworkDevices();

        // get statistics and bandwidth property
        for (Switch swc : switches) {
            Node node = swc.getNode();
            List<NodeConnectorStatistics> stat = statsManager
                .getNodeConnectorStatistics(node);

            Map<NodeConnector, Data> node_data = new HashMap();

            for (NodeConnector nc : swc.getNodeConnectors()) {
                Map<String,Property> mp =
                    switchManager.getNodeConnectorProps(nc);

                Data data = new Data();

                data.setNodeConnector(nc);
                data.setBandwidth(mp.get(propertyName).getStringValue());

                node_data.put(nc, data);
            }

            for (NodeConnectorStatistics ncs : stat) {
                Data data = node_data.get(ncs.getNodeConnector());

                if (data == null) {
                    // statistics about switch<->controller
                    continue;
                }

                long sent = ncs.getReceivePacketCount() + ncs.getTransmitPacketCount();
                long drop = ncs.getReceiveDropCount() + ncs.getTransmitDropCount();

                data.setPacketDrop(drop);
                data.setPacketSent(sent);

                node_data.put(ncs.getNodeConnector(), data);
            }

            List<Data> list_data = new ArrayList();

            for (NodeConnector key : node_data.keySet()) {
                list_data.add(node_data.get(key));
            }

            edge.put(node, list_data);

        }

        // get topology information
        Map<Node,Set<Edge>> topology = topologyManager.getNodeEdges();

        for (Node key : edge.keySet()) {

            for (Edge e : topology.get(key)) {

                NodeConnector tail_nc = e.getTailNodeConnector();
                NodeConnector head_nc = e.getHeadNodeConnector();

                Node tail = tail_nc.getNode();
                Node head = head_nc.getNode();

                // head -> tail
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
            for (Data n : edge.get(key)) {
                List<Host> hosts = topologyManager
                    .getHostsAttachedToNodeConnector(n.getNodeConnector());

                if (hosts != null) {
                    n.setHosts(hosts);
                }
            }
        }

        // print info
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
                System.out.println("\tBandwidth: " + data.getBandwidth());
                System.out.println("");
            }
        }

        return;
    }
}
