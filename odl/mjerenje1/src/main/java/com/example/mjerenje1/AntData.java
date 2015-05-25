package com.example.mjerenje1;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;

public class AntData extends Ant {
    private Map<Node, Double> packetLoss;
    private double a = 5.0;
    private double b = 1.0;
    private double c = 1.0;

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

        this.cost = this.a - Math.log(this.b * this.c * (1 - Pe2e));
    }
}
