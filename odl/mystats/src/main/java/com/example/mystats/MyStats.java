package com.example.mystats;

import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.example.statscollector.IStatsCollector;
import com.example.statscollector.Data;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyStats{
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

    void getFlowStatistics() {
        String containerName = "default";

        Map<Node, List<Data> > edge = new HashMap();
        Map<Long, Map<Node, List<Data> > > res;

        ITopologyManager topologyManager = (ITopologyManager) ServiceHelper
            .getInstance(ITopologyManager.class, containerName, this);

        IStatsCollector statsCollector = (IStatsCollector) ServiceHelper
            .getInstance(IStatsCollector.class, containerName, this);

        if (statsCollector != null) {
            // if the statscollector bundle is present get the latest stats
            res = statsCollector.getStats();

            Long latest = null;
            for (Long timestamp : res.keySet()) {
                if (latest == null || timestamp > latest) {
                    edge = res.get(timestamp);
                    latest = timestamp;
                }
            }

            System.out.println("Time: " + latest);
        } else {
            // do nothing if the stats collector bundle is not present
            System.out.println("StatsCollector not present!");
            return;
        }


        // get topology information
        Map<Node,Set<Edge>> topology = topologyManager.getNodeEdges();

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

        /* TODO run A* on collected data */
        /* TODO install a new flow using flowManager */

        return;
    }
}
