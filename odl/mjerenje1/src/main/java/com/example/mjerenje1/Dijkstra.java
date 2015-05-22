package com.example.mjerenje1;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.PriorityQueue;
import java.util.Collections;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;

import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;

public final class Dijkstra {
    private static class myVertex implements Comparable<myVertex> {
        public final Node node;
        public List<myEdge> adj;

        public double distance = Double.POSITIVE_INFINITY;
        public myVertex prev = null;

        public myVertex(Node node) {
            this.node = node;
            adj = new ArrayList<myEdge>();
        }

        public int compareTo(myVertex other) {
            return Double.compare(distance, other.distance);
        }
    }

    private static class myEdge {
        public final myVertex target;
        public final double cost;

        public myEdge(myVertex target, double cost) {
            this.target = target;
            this.cost = cost;
        }
    }

    public static List<Node> reconstructPath(myVertex v) {
        List<Node> path = new ArrayList<Node>();

        for (myVertex u = v; u != null; u = u.prev) {
            path.add(u.node);
        }
        Collections.reverse(path);

        return path;
    }

    public static List<Edge> getPathHopByHop(Node source, Node goal,
            Map<Node, Set<Edge> > edges) {
        long start_time = System.nanoTime();
        /* prepare data structures for dijkstra */
        Map<Node, myVertex> node2vertex = new HashMap<Node, myVertex>();

        for (Node n: edges.keySet()) {
            node2vertex.put(n, new myVertex(n));
        }

        for (Node n: edges.keySet()) {
            for (Edge e: edges.get(n)) {
                myVertex u, v;

                u = node2vertex.get(e.getTailNodeConnector().getNode());
                v = node2vertex.get(e.getHeadNodeConnector().getNode());

                u.adj.add(new myEdge(v, 1.0));
            }
        }
        myVertex start = node2vertex.get(source);
        start.distance = 0.0;

        myVertex end = node2vertex.get(goal);

        List<Edge> ret = getPathInternal(start, end, edges);
        long end_time = System.nanoTime();

        System.out.println("[DIJ][TIME] " + ((end_time - start_time) / 1000.) + " us");
        return ret;
    }

    public static List<Edge> getPathVideo(Node source, Node goal,
            Map<Node, Set<Edge> > edges, IStatisticsManager statsManager) {

        long start_time = System.nanoTime();
        /* prepare data structures for dijkstra */
        Map<Node, myVertex> node2vertex = new HashMap<Node, myVertex>();

        for (Node n: edges.keySet()) {
            node2vertex.put(n, new myVertex(n));
        }

        for (Node n: edges.keySet()) {
            for (Edge e: edges.get(n)) {
                myVertex u, v;

                u = node2vertex.get(e.getTailNodeConnector().getNode());
                v = node2vertex.get(e.getHeadNodeConnector().getNode());

                NodeConnectorStatistics stat = statsManager
                    .getNodeConnectorStatistics(e.getTailNodeConnector());

                double loss_u = (stat.getTransmitDropCount() +
                                 stat.getReceiveDropCount()) /
                                (stat.getTransmitPacketCount() +
                                 stat.getReceivePacketCount());

                double loss = -1.0 * Math.log(1 - loss_u);

                u.adj.add(new myEdge(v, loss));
            }
        }
        myVertex start = node2vertex.get(source);
        start.distance = 0.0;

        myVertex end = node2vertex.get(goal);

        List<Edge> ret = getPathInternal(start, end, edges);
        long end_time = System.nanoTime();

        System.out.println("Time elapsed: " + ((end_time - start_time) / 1000.) + " us");
        return ret;
    }

    private static List<Edge> getPathInternal(myVertex start, myVertex end,
            Map<Node, Set<Edge> > edges) {
        PriorityQueue<myVertex> q = new PriorityQueue<myVertex>();
        q.add(start);

        while (!q.isEmpty()) {
            myVertex u = q.poll();

            if (end.node.equals(u.node)) {
                /* reconstruct the path */
                // System.out.println("Path found, d = " + u.distance);

                List<Node> path = reconstructPath(u);
                List<Edge> ret = new ArrayList<Edge>();

                for (int i = 0; i < path.size() - 1; ++i) {
                    Node head = path.get(i);
                    Node tail = path.get(i + 1);

                    for (Edge e: edges.get(head)) {
                        if (tail.equals(e.getTailNodeConnector().getNode())) {
                            ret.add(e);
                            break;
                        }
                    }
                }
                return ret;
            }

            for (myEdge e: u.adj) {
                myVertex v = e.target;

                if (u.distance + e.cost < v.distance) {
                    q.remove(v);
                    v.distance = u.distance + e.cost;
                    v.prev = u;
                    q.add(v);
                }
            }
        }

        // System.out.println("Path not found!");
        return null;
    }
}
