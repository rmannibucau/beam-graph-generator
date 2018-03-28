package com.github.rmannibucau.beam.graph;

import javax.swing.Icon;

public class Node extends Info {

    private Icon icon;

    public Node(String txt) {
        super(txt);
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }
}
