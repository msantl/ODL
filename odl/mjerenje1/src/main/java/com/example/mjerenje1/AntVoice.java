package com.example.mjerenje1;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;

public class AntVoice extends Ant {
    private Map<Node, Double> packetLoss;
    private Map<Node, Double> delay;

    private double T = 5.0;
    private double alfa = 1.0;
    private double beta = 1.0;
    private double gama = 1.0;
    private double theta = 1.0;

    public AntVoice(Map<Node, Double> packetLoss, Map<Node, Double> delay) {
        super();
        this.packetLoss = packetLoss;
        this.delay = delay;
    }

    public void calculateCost() {
        List<Node> path = this.getPath();
        double Pe2e = 0.0;
        double De2e = 0.0;

        for (Node node: path) {
            Pe2e = 1 - (1 - Pe2e) * (1 - this.packetLoss.get(node));
            De2e = De2e + this.delay.get(node);
        }

        this.cost = this.T - this.alfa * Pe2e +
                    this.beta * De2e -
                    this.gama * Math.pow(De2e, 2.0) +
                    this.theta * Math.pow(De2e, 3.0);
    }

}
