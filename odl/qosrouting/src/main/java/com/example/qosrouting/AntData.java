package com.example.qosrouting;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;

public class AntData extends Ant {
    private Map<Node, Double> packetLoss;
    public double a = 10.9272;
    public double b = 258.117;
    public double c = 21.8544;

    public AntData(Map<Node, Double> packetLoss) {
        super();
        this.packetLoss = packetLoss;
    }

    public void calculateCost() {
        List<Node> path = this.getPath();
        double Pe2e = 0.0;

        for (Node node: path) {
            Pe2e = 1 - (1 - Pe2e) * (1 - this.packetLoss.get(node));
        }

        this.cost = this.a * Math.log(this.b * (1 - Pe2e));
    }
}
