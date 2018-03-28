package com.github.rmannibucau.beam.graph;

import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class Diagram extends DirectedSparseGraph<Node, Edge> {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
