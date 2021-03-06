package com.example.mjerenje1;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;

public class AntVideo extends Ant {
    private Map<Node, Double> packetLoss;
    public double P = 3.53;
    public double Q = 0.035;

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
