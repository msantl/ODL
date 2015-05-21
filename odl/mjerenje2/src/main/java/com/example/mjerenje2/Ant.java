package com.example.mjerenje2;

import java.util.List;
import java.util.ArrayList;

import org.opendaylight.controller.sal.core.Node;

public abstract class Ant {
    public List<Node> path;
    public Double     cost;

    public Ant() {
        this.path = new ArrayList<Node>();
        this.cost = null;
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
