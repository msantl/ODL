package com.example.qosrouting;

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

    protected Random random;
    protected Node source;
    protected Node destination;

    protected Map<Node, Integer> node2int;

    private final double alpha = 1.0;
    private final double beta = 2.0;
    private final double evaporationRate = 0.7;

    private final int iterations = 10;
    private final int antPopulation = 5;

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

        this.pheromones = new double[size][size];
        this.nearness = new double[size][size];

        this.random = new Random();

        /* we are maximizing the MOS of each path */
        this.path = null;
        this.trail = null;

        /* helper map */
        node2int = new HashMap<Node, Integer>();
        int id = 0;
        for (Node node: graph.keySet()) {
            node2int.put(node, id);
            id += 1;
        }
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
            System.out.println("Traffic type not set");
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

    private void antRun(Ant ant) {
        Node curr = this.source;
        ant.addToPath(curr);

        while (!curr.equals(this.destination)) {
            Node next = null;
            Map<Node, Double> prob = this.calculateProbability(ant, curr);

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
    }

    public void run() {
        List<Ant> ants = new ArrayList<Ant>();
        /* while termination criterion is no met */
        for (int it = 0; it < this.iterations; ++it) {
            System.out.println("Iteration: " + it);
            /* generate and release the ants */
            for (int pop = 0; pop < this.antPopulation; ++pop) {
                System.out.print(".");
                Ant ant = this.generateAnt();

                /* find a way for each ant */
                this.antRun(ant);

                /* let each ant calculate its own cost */
                ant.calculateCost();
                /* keep track of this ant */
                ants.add(ant);
            }

            System.out.println("Evaporating pheromones");
            /* evaporate pheromones */
            this.evaporatePheromones();
            /* add pheromones from ant trails */
            double sum = 0.0;
            for (Ant ant: ants) {
                sum += ant.getCost();
            }

            System.out.println("Enhancing pheromones");
            for (Ant ant: ants) {
                this.enhancePheromones(ant.getPath(), ant.getCost() / sum);
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

        this.reconstructTrail();
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
