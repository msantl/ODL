package com.example.mjerenje1;

import java.util.Map;
import java.util.List;

import org.opendaylight.controller.sal.core.Node;

public class MOS{
    public static double a = 10.9272;
    public static double b = 258.117;
    public static double c = 21.8544;

    public static double P = 3.53;
    public static double Q = 0.035;

    public static double T = 4.3;
    public static double alpha = 19.5;
    public static double beta = 0.0;
    public static double gamma = 0.0186;
    public static double delta = 0.0;

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

        return T - alpha * Pe2e + beta * De2e - gamma *
                Math.pow(De2e, 2.) + delta * Math.pow(De2e, 3.);
    }

    public static double Data(List<Node> path, Map<Node, Double> packet_loss) {
        double Pe2e = 0.0;

        for (Node node: path) {
            Pe2e = 1 - (1 - Pe2e) * (1 - packet_loss.get(node));
        }

        return a * Math.log10(b * (1 - Pe2e)) - c;
    }
}
