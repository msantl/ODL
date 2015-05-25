package com.example.qosrouting;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;

public class AntVideo extends Ant {
    private Map<Node, Double> packetLoss;
    private double P = 4.0;
    private double Q = 1.0;

    public AntVideo(Map<Node, Double> packetLoss) {
        super();
        this.packetLoss = packetLoss;
    }

    public void calculateCost() {
        List<Node> path = this.getPath();
        double Pe2e = 0.0;

        for (Node node: path) {
            Pe2e = 1 - (1 - Pe2e) * (1 - this.packetLoss.get(node));
        }
        this.cost = 1 + this.P * Math.exp(-1.0 * Pe2e / this.Q);
    }
}
