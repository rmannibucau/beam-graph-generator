package com.github.rmannibucau.beam.graph;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.Icon;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.BasicEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;

public class GraphViewer extends VisualizationViewer<Node, Edge> {

    public GraphViewer(final Layout<Node, Edge> nodeEdgeLayout) {
        super(nodeEdgeLayout);
        init();
    }

    private void init() {
        setOpaque(true);
        setBackground(Color.WHITE);

        final Font font = new Font("Serif", Font.PLAIN, 20);

        getRenderContext().setVertexFillPaintTransformer(new VertexFillPaintTransformer());
        getRenderContext().setVertexShapeTransformer(new VertexShapeTransformer(getFontMetrics(font)));
        getRenderContext().setVertexIconTransformer(new VertexIconTransformer());
        getRenderContext().setVertexLabelTransformer(new VertexLabelTransformer());
        getRenderContext().setVertexFontTransformer(new VertexFontTransformer(font));
        getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);

        getRenderContext().setEdgeLabelTransformer(new EdgeLabelTransformer());
        getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<>());
        getRenderContext().setEdgeLabelClosenessTransformer(new EdgeLabelClosenessTransformer());
        getRenderContext().getEdgeLabelRenderer().setRotateEdgeLabels(false);
        getRenderer().setEdgeLabelRenderer(new EdgeLabelRenderer());
    }

    public static class EdgeLabelRenderer extends BasicEdgeLabelRenderer<Node, Edge> {

        @Override
        public void labelEdge(final RenderContext<Node, Edge> rc, final Layout<Node, Edge> layout, final Edge e,
                final String label) {
            if (label == null || label.length() == 0) {
                return;
            }

            final Graph<Node, Edge> graph = layout.getGraph();
            // don't draw edge if either incident vertex is not drawn
            final Pair<Node> endpoints = graph.getEndpoints(e);
            final Node v1 = endpoints.getFirst();
            final Node v2 = endpoints.getSecond();
            if (!rc.getEdgeIncludePredicate().evaluate(Context.getInstance(graph, e))) {
                return;
            }

            if (!rc.getVertexIncludePredicate().evaluate(Context.getInstance(graph, v1))
                    || !rc.getVertexIncludePredicate().evaluate(Context.getInstance(graph, v2))) {
                return;
            }

            final Point2D p1 = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, layout.transform(v1));
            final Point2D p2 = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, layout.transform(v2));

            final GraphicsDecorator g = rc.getGraphicsContext();
            final Component component = prepareRenderer(rc, rc.getEdgeLabelRenderer(), label, rc.getPickedEdgeState().isPicked(e),
                    e);
            final Dimension d = component.getPreferredSize();

            final AffineTransform old = g.getTransform();
            final AffineTransform xform = new AffineTransform(old);
            final FontMetrics fm = g.getFontMetrics();
            final int w = fm.stringWidth(e.getText());
            final double p = Math.max(0, p1.getX() + p2.getX() - w);
            xform.translate(Math.min(layout.getSize().width - w, p / 2), (p1.getY() + p2.getY() - fm.getHeight()) / 2);
            g.setTransform(xform);
            g.draw(component, rc.getRendererPane(), 0, 0, d.width, d.height, true);

            g.setTransform(old);
        }
    }

    private static class EdgeLabelClosenessTransformer implements Transformer<Context<Graph<Node, Edge>, Edge>, Number> {

        @Override
        public Number transform(Context<Graph<Node, Edge>, Edge> context) {
            return 0.5;
        }
    }

    private static class EdgeLabelTransformer implements Transformer<Edge, String> {

        @Override
        public String transform(Edge i) {
            return i.getText();
        }
    }

    private static class VertexFontTransformer implements Transformer<Node, Font> {

        private final Font font;

        private VertexFontTransformer(final Font font) {
            this.font = font;
        }

        @Override
        public Font transform(final Node node) {
            // todo: config
            return font;
        }
    }

    private static class VertexLabelTransformer extends ToStringLabeller<Node> {

        @Override
        public String transform(final Node node) {
            if (node.getIcon() != null) {
                return null;
            }
            return node.getText();
        }
    }

    private static class VertexIconTransformer implements Transformer<Node, Icon> {

        @Override
        public Icon transform(final Node v) {
            if (v.getIcon() != null) {
                return v.getIcon();
            }
            return null;
        }
    }

    private static class VertexFillPaintTransformer implements Transformer<Node, Paint> {

        @Override
        public Paint transform(Node node) {
            return Color.WHITE;
        }
    }

    private static class VertexShapeTransformer implements Transformer<Node, Shape> {

        private static final int X_MARGIN = 4;

        private static final int Y_MARGIN = 2;

        private FontMetrics metrics;

        private VertexShapeTransformer(final FontMetrics f) {
            metrics = f;
        }

        @Override
        public Shape transform(Node i) {
            int w;
            int h;
            Icon icon = i.getIcon();
            if (icon == null) {
                w = metrics.stringWidth(i.getText());
                h = metrics.getHeight();
            } else {
                w = icon.getIconWidth();
                h = icon.getIconHeight();
            }

            h += Y_MARGIN;
            w += X_MARGIN;

            // centering
            final AffineTransform transform = AffineTransform.getTranslateInstance(-w / 2.0, -h / 2.0);
            return transform.createTransformedShape(new Rectangle(0, 0, w, h));
        }
    }
}
