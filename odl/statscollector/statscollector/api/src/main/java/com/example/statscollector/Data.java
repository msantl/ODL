package com.example.statscollector;

import java.util.List;
import java.util.ArrayList;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Host;

public class Data {
    private Node node;
    private List<Host> host;
    private NodeConnector nc;
    private long pkt_drop;
    private long pkt_sent;
    private long tx_count;
    private long rx_count;
    private String bandwidth;

    public Data() {
        this.node = null;
        this.host = null;
        this.nc = null;
        this.bandwidth = null;
    }

    public void setNode(Node node) {this.node = node;}
    public void setNodeConnector(NodeConnector nc) {this.nc = nc;}
    public void setPacketDrop(long pkt_drop) {this.pkt_drop = pkt_drop;}
    public void setPacketSent(long pkt_sent) {this.pkt_sent = pkt_sent;}
    public void setTxCount(long tx_count) {this.tx_count = tx_count;}
    public void setRxCount(long rx_count) {this.rx_count = rx_count;}
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
    public long getTxCount() {return this.tx_count;}
    public long getRxCount() {return this.rx_count;}
    public String getBandwidth() {return this.bandwidth;}
    public List<Host> getHosts() {return this.host;}

    @Override
    public String toString() {
        String ret = "Data";
        if (getNode() != null) {
            ret += "\tNode: " + getNode().toString();
        }
        if (getHosts() != null) {
            ret += "\tHosts: ";
            for (Host h : getHosts()) {
                ret += h.getNetworkAddressAsString() + ", ";
            }
        }

        if (getNodeConnector() != null) {
            ret += "\tNodeConnector: " + getNodeConnector().getNodeConnectorIdAsString();
        }

        ret += "\tPacketDrop: " + getPacketDrop();
        ret += "\tPacketSent: " + getPacketSent();

        ret += "\tTx count: " + getTxCount();
        ret += "\tRx count: " + getRxCount();

        if (getBandwidth() != null) {
            ret += "\tBandwidth: " + getBandwidth();
        }
        return ret;
    }
}

