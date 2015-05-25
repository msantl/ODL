package com.example.mjerenje1;

import java.util.Map;
import java.util.List;

import org.opendaylight.controller.sal.core.Node;

public class MOS{
    public static double a = 5.0;
    public static double b = 1.0;
    public static double c = 1.0;

    public static double P = 4.0;
    public static double Q = 1.0;

    public static double T = 5.0;
    public static double alpha = 1.0;
    public static double beta = 1.0;
    public static double gamma = 1.0;
    public static double delta = 1.0;


    public static double Video(List<Node> path, Map<Node, Double> packet_loss) {
        double Pe2e = 0.0;

        for (Node node: path) {
            Pe2e = 1 - (1 - Pe2e) * (1 - packet_loss.get(node));
        }

        return 1 + P * Math.exp(-1.0 * Pe2e / Q);
    }

    public static double Audio(List<Node> path, Map<Node, Double> packet_loss,
            Map<Node, Double> delay) {
        double De2e = 0.0, Pe2e = 0.0;

        for (Node node: path) {
            Pe2e = 1 - (1 - Pe2e) * (1 - packet_loss.get(node));
            De2e = De2e + delay.get(node);
        }

        return T - (alpha * Pe2e + beta * De2e - gamma *
                Math.pow(De2e, 2.) + delta * Math.pow(De2e, 3.));
    }

    public static double Data(List<Node> path, Map<Node, Double> packet_loss) {
        double Pe2e = 0.0;

        for (Node node: path) {
            Pe2e = 1 - (1 - Pe2e) * (1 - packet_loss.get(node));
        }

        return a - Math.log(b * c * (1 - Pe2e));
    }
}
