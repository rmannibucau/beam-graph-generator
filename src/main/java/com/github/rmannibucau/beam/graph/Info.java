package com.github.rmannibucau.beam.graph;

public abstract class Info {

    private String text;

    public Info(String txt) {
        text = txt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + getText() + "}";
    }
}
