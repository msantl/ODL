package com.example.qosrouting;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;

public class AntVoice extends Ant {
    private Map<Node, Double> packetLoss;
    private Map<Node, Double> delay;

    public double T = 4.3;
    public double alpha = 19.5;
    public double beta = 0.0;
    public double gamma = 0.0186;
    public double delta = 0.0;

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

        this.cost = this.T - this.alpha * Pe2e +
                    this.beta * De2e -
                    this.gamma * Math.pow(De2e, 2.0) +
                    this.delta * Math.pow(De2e, 3.0);
    }

}
