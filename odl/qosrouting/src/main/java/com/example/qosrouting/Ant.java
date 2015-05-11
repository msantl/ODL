package com.example.qosrouting;

import java.util.List;
import java.util.ArrayList;

import org.opendaylight.controller.sal.core.Node;

public abstract class Ant {
    public List<Node> path;
    public double     cost;

    public Ant() {
        this.path = new ArrayList<Node>();
    }

    public List<Node> getPath() {
        return this.path;
    }

    public void addToPath(Node node) {
        this.path.add(node);
    }

    public boolean hasVisited(Node node) {
        return this.path.contains(node);
    }

    public abstract void calculateCost();

    public double getCost() {
        return this.cost;
    }
}
