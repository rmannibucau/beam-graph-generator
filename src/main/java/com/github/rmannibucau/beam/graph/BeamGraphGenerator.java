package com.github.rmannibucau.beam.graph;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.Read;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.ApplicationNameOptions;
import org.apache.beam.sdk.runners.TransformHierarchy;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PValue;

import com.github.rmannibucau.beam.graph.runner.GraphRunner;

public class BeamGraphGenerator {

    private final GraphRunner.Config config;

    public BeamGraphGenerator(final GraphRunner.Config config) {
        this.config = config;
    }

    public Diagram generate(final Pipeline pipeline) {
        final GraphPipelineVisitor visitor = new GraphPipelineVisitor();
        pipeline.traverseTopologically(visitor);
        visitor.graph.setName(ofNullable(config.as(ApplicationNameOptions.class).getAppName()).orElse("Pipeline Model"));
        return visitor.graph;
    }

    private static class GraphPipelineVisitor implements Pipeline.PipelineVisitor {

        private Diagram graph;

        private final Map<String, Node> nodes = new HashMap<>();

        @Override
        public void enterPipeline(final Pipeline p) {
            graph = new Diagram();
        }

        @Override
        public CompositeBehavior enterCompositeTransform(final TransformHierarchy.Node node) {
            return CompositeBehavior.ENTER_TRANSFORM;
        }

        @Override
        public void visitPrimitiveTransform(final TransformHierarchy.Node node) {
            final Node currentNode = new Node(node.getFullName() + (node.getTransform() != null ?  " (" + toClass(node.getTransform()) + ")" : ""));
            graph.addVertex(currentNode);
            node.getOutputs().values().forEach(output -> nodes.put(output.getName(), currentNode));
            node.getInputs().values().forEach(input -> {
                final Node from = nodes.get(input.getName());
                if (from == null) {
                    throw new IllegalArgumentException("Didn't find " + input.getName());
                }
                graph.addEdge(new Edge(""), from, currentNode);
            });
        }

        private String toClass(final PTransform<?, ?> transform) {
            if (Read.Bounded.class.isInstance(transform)) {
                return Read.Bounded.class.cast(transform).getSource().getClass().getName();
            }
            if (Read.Unbounded.class.isInstance(transform)) {
                return Read.Unbounded.class.cast(transform).getSource().getClass().getName();
            }
            if (ParDo.MultiOutput.class.isInstance(transform)) {
                return ParDo.MultiOutput.class.cast(transform).getFn().getClass().getName();
            }
            if (ParDo.SingleOutput.class.isInstance(transform)) {
                return ParDo.SingleOutput.class.cast(transform).getFn().getClass().getName();
            }
            return transform.getClass().getSimpleName();
        }

        @Override
        public void leaveCompositeTransform(final TransformHierarchy.Node node) {
            // no-op
        }

        @Override
        public void visitValue(final PValue value, final TransformHierarchy.Node producer) {
            // no-op
        }

        @Override
        public void leavePipeline(final Pipeline pipeline) {
            // no-op
        }
    }
}
