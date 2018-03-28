package com.github.rmannibucau.beam.graph;

import static org.junit.Assert.assertTrue;

import java.io.File;

import com.github.rmannibucau.beam.graph.runner.GraphRunner;

import org.junit.Test;

public class BeamGraphGeneratorTest {
    @Test
    public void generate() {
        final File graph = new File("target/BeamGraphGeneratorTest_generate.png");
        WordCount.main(new String[]{
                "--graphOutput=" + graph.getAbsolutePath(),
                "--output=ignored",
                "--graphWidth=1884",
                "--graphHeight=1632",
                "--graphShow=false",
                "--runner=" + GraphRunner.class.getName()
        });
        assertTrue(graph.exists());
    }
}
