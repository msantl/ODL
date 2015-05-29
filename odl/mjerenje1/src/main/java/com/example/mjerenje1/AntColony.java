package com.example.mjerenje1;

import java.util.Random;

import java.util.List;
import java.util.ArrayList;

import java.util.Set;

import java.util.Map;
import java.util.HashMap;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;

public final class AntColony {
    protected double[][] pheromones;
    protected double[][] nearness;
    protected List<Node> path;
    protected List<Edge> trail;
    protected Map<Node, Set<Edge> > graph;
    protected int size;
    protected Map<Node, Double> packetLoss;
    protected Map<Node, Double> delay;
    protected double MOS;

    protected Random random;
    protected Node source;
    protected Node destination;

    protected Map<Node, Integer> node2int;
    protected Map<Integer, Node> int2node;

    private final double alpha = 1.0;
    private final double beta = 1.0;

    private final int iterations = 10;
    private final int antPopulation = 15;
    private final double evaporationRate = 0.5;

    private TrafficType trafficType;

    public AntColony(Map<Node, Set<Edge> > graph,
            Node source, Node destination, TrafficType trafficType,
            Map<Node, Double> packetLoss, Map<Node, Double> delay) {
        /* initialize with given values */
        this.graph = graph;
        this.size = graph.keySet().size();

        this.source = source;
        this.destination = destination;

        this.trafficType = trafficType;

        this.packetLoss = packetLoss;
        this.delay = delay;

        /* helper map */
        node2int = new HashMap<Node, Integer>();
        int2node = new HashMap<Integer, Node>();
        int id = 0;
        for (Node node: graph.keySet()) {
            node2int.put(node, id);
            int2node.put(id, node);
            id += 1;
        }

        /* initialize trails */
        this.pheromones = new double[size][size];
        this.nearness = new double[size][size];

        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                this.pheromones[i][j] = 0.5;
                this.nearness[i][j] = (1 - packetLoss.get(int2node.get(i))) *
                                      (1 - packetLoss.get(int2node.get(j)));
            }
        }

        this.random = new Random();

        /* we are maximizing the MOS of each path */
        this.path = null;
        this.trail = null;
    }

    private void evaporatePheromones() {
        for (int i = 0; i < this.size; ++i) {
            for (int j = 0; j < this.size; ++j) {
                this.pheromones[i][j] *= (1 - this.evaporationRate);
            }
        }
    }

    private void enhancePheromones(List<Node> path, double value) {
        Node prev = null;
        for (Node node: path) {
            if (prev == null) {
                prev = node;
                continue;
            }

            int i = node2int.get(prev);
            int j = node2int.get(node);

            prev = node;

            this.pheromones[i][j] += value;
        }
        return;
    }

    private Ant generateAnt() {
        Ant ant = null;

        if (this.trafficType == TrafficType.VOICE) {
            ant = new AntVoice(this.packetLoss, this.delay);
        } else if (this.trafficType == TrafficType.VIDEO) {
            ant = new AntVideo(this.packetLoss);
        } else if (this.trafficType == TrafficType.DATA) {
            ant = new AntData(this.packetLoss);
        } else {
            //System.out.println("Traffic type not set");
        }

        return ant;
    }

    private Map<Node, Double> calculateProbability(Ant ant, Node src) {
        Map<Node, Double> ret = new HashMap<Node, Double>();
        /* calculate probabilities */

        double sum = 0.0;
        for (Edge edge: this.graph.get(src)) {
            Node dst = edge.getTailNodeConnector().getNode();
            if (dst.equals(src)) {
                continue;
            }

            if (ant.hasVisited(dst)) {
                continue;
            }

            int i = this.node2int.get(src);
            int j = this.node2int.get(dst);

            sum += Math.pow(this.pheromones[i][j], this.alpha) *
                   Math.pow(this.nearness[i][j], this.beta);
        }

        if (Math.abs(sum) < 1e-9) {
            return null;
        }

        for (Edge edge: this.graph.get(src)) {
            Node dst = edge.getTailNodeConnector().getNode();
            if (dst.equals(src)) {
                continue;
            }

            if (ant.hasVisited(dst)) {
                continue;
            }

            int i = this.node2int.get(src);
            int j = this.node2int.get(dst);

            double prob = Math.pow(this.pheromones[i][j], this.alpha) *
                          Math.pow(this.nearness[i][j], this.beta) /
                          sum;

            ret.put(dst, prob);
        }

        return ret;
    }

    /*
     * returns true if a path has been found, false otherwise
     */
    private boolean antRun(Ant ant) {
        Node curr = this.source;
        ant.addToPath(curr);

        //System.out.print("Path: ");
        while (!curr.equals(this.destination)) {
            Node next = null;
            Map<Node, Double> prob = this.calculateProbability(ant, curr);

            //System.out.print(" -> " + curr.toString());

            if (prob == null) {
                //System.out.println("FAIL");
                return false;
            }

            double rand = this.random.nextDouble();
            for (Node n: prob.keySet()) {
                if (rand > prob.get(n)) {
                    rand -= prob.get(n);
                } else {
                    next = n;
                    break;
                }
            }

            ant.addToPath(next);
            curr = next;
        }

        //System.out.println(" -> " + curr.toString() + " OK");

        return true;
    }

    public void run() {
        long start_time = System.nanoTime();

        List<Ant> ants = null;
        /* while termination criterion is no met */
        for (int it = 0; it < this.iterations; ++it) {
            ants = new ArrayList<Ant>();
            //System.out.println("Iteration: " + it);
            /* generate and release the ants */
            for (int pop = 0; pop < this.antPopulation; ++pop) {
                Ant ant = this.generateAnt();

                /* find a way for each ant */
                if (this.antRun(ant)) {
                    //System.out.println("Found a path for ant: " + pop);
                    /* let each ant calculate its own cost */
                    ant.calculateCost();
                    /* keep track of this ant */
                    ants.add(ant);
                } else {
                    //System.out.println("Failed to find a path for ant: " + pop);
                }
            }

            //System.out.println("Evaporating pheromones");
            /* evaporate pheromones */
            this.evaporatePheromones();
            /* add pheromones from ant trails */
            double sum = 0.0;
            for (Ant ant: ants) {
                sum += ant.getCost();
            }

            if (Math.abs(sum) < 1e-9) {
                //System.out.println("Enhancing pheromones");
                for (Ant ant: ants) {
                    this.enhancePheromones(ant.getPath(), ant.getCost() / sum);
                }
            }
        }

        /* take the best ant's path as the reuslt */
        Double best_cost = null;
        for (Ant ant: ants) {
            if (best_cost == null || best_cost < ant.getCost()) {
                best_cost = ant.getCost();
                this.path = ant.getPath();
            }
        }

        if (best_cost != null) {
            this.MOS = best_cost;
            this.reconstructTrail();
        }

        long end_time = System.nanoTime();
        System.out.println("[ACO][TIME] " + ((end_time - start_time) / 1000.) + " us");
    }

    private void reconstructTrail() {
        this.trail = new ArrayList<Edge>();
        List<Node> path = this.path;

        for (int i = 0; i < path.size() - 1; ++i) {
            Node head = path.get(i);
            Node tail = path.get(i + 1);

            for (Edge e: this.graph.get(head)) {
                if (tail.equals(e.getTailNodeConnector().getNode())) {
                    this.trail.add(e);
                    break;
                }
            }
        }
    }

    public List<Edge> getPath() {
        return this.trail;
    }

}
