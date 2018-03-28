package com.github.rmannibucau.beam.graph.runner;

import static java.util.Collections.emptyList;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.PipelineRunner;
import org.apache.beam.sdk.metrics.DistributionResult;
import org.apache.beam.sdk.metrics.GaugeResult;
import org.apache.beam.sdk.metrics.MetricQueryResults;
import org.apache.beam.sdk.metrics.MetricResult;
import org.apache.beam.sdk.metrics.MetricResults;
import org.apache.beam.sdk.metrics.MetricsFilter;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsRegistrar;
import org.apache.beam.sdk.options.Validation;
import org.joda.time.Duration;

import com.github.rmannibucau.beam.graph.BeamGraphGenerator;
import com.github.rmannibucau.beam.graph.Diagram;
import com.github.rmannibucau.beam.graph.Edge;
import com.github.rmannibucau.beam.graph.GraphViewer;
import com.github.rmannibucau.beam.graph.LevelLayout;
import com.github.rmannibucau.beam.graph.Node;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;

public class GraphRunner extends PipelineRunner<GraphRunner.GraphResult> {

    private final Config config;

    private GraphRunner(final Config config) {
        this.config = config;
    }

    @Override
    public GraphResult run(final Pipeline pipeline) {
        final Diagram diagram = new BeamGraphGenerator(config).generate(pipeline);
        render(diagram);
        return new GraphResult(diagram);
    }

    private void render(final Diagram diagram) {
        final Dimension outputSize = new Dimension(config.getGraphWidth() < 0 ? 1000 : config.getGraphWidth(), config.getGraphHeight() < 0 ? 1000 : config.getGraphHeight());
        final LevelLayout layout = new LevelLayout(diagram);
        final VisualizationViewer<Node, Edge> viewer = new GraphViewer(layout);

        layout.setVertexShapeTransformer(viewer.getRenderContext().getVertexShapeTransformer());
        layout.setSize(outputSize);
        layout.setIgnoreSize(config.getGraphAdjust());
        layout.reset();
        viewer.setPreferredSize(layout.getSize());
        viewer.setSize(layout.getSize());

        // creating a realized window to be sure the viewer will be able to draw correctly the graph
        final JFrame window = createWindow(viewer, diagram.getName());

        // saving it too
        final File output = new File(config.getGraphOutput());
        if (!output.exists()) {
            output.getParentFile().mkdirs();
        }
        saveView(layout.getSize(), outputSize, diagram.getName(), viewer, output);

        if (config.getGraphShow()) {
            final CountDownLatch latch = new CountDownLatch(1);
            final CloseWindowWaiter waiter = new CloseWindowWaiter(latch);
            window.setVisible(true);
            window.addWindowListener(waiter);
            try {
                latch.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            window.dispose();
        }
    }

    private JFrame createWindow(VisualizationViewer<Node, Edge> viewer, String name) {
        viewer.setBackground(Color.WHITE);

        DefaultModalGraphMouse<Node, Edge> gm = new DefaultModalGraphMouse<Node, Edge>();
        gm.setMode(DefaultModalGraphMouse.Mode.PICKING);
        viewer.setGraphMouse(gm);

        JFrame frame = new JFrame(name + " viewer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new GridLayout());
        frame.getContentPane().add(viewer);
        frame.pack();

        return frame;
    }

    private void saveView(Dimension currentSize, Dimension desiredSize, String name, VisualizationViewer<Node, Edge> viewer,
            final File output) {
        BufferedImage bi = new BufferedImage(currentSize.width, currentSize.height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        boolean db = viewer.isDoubleBuffered();
        viewer.setDoubleBuffered(false);
        viewer.paint(g);
        viewer.setDoubleBuffered(db);
        if (!currentSize.equals(desiredSize)) {
            // todo: log
            double xFactor = desiredSize.width * 1. / currentSize.width;
            double yFactor = desiredSize.height * 1. / currentSize.height;
            double factor = Math.min(xFactor, yFactor);
            // getLog().info("optimal size is (" + currentSize.width + ", " + currentSize.height + ")");
            // getLog().info("scaling with a factor of " + factor);

            final AffineTransform tx = new AffineTransform();
            tx.scale(factor, factor);
            final AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
            final BufferedImage biNew = new BufferedImage((int) (bi.getWidth() * factor), (int) (bi.getHeight() * factor),
                    bi.getType());
            bi = op.filter(bi, biNew);
        }
        g.dispose();

        OutputStream os = null;
        try {
            os = new FileOutputStream(output);
            if (!ImageIO.write(bi, "png", os)) {
                throw new IllegalStateException("can't save picture " + output.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new IllegalStateException("can't save the diagram", e);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    throw new IllegalStateException("can't close diagram", e);
                }
            }
        }
    }

    public static GraphRunner fromOptions(final PipelineOptions opts) {
        return new GraphRunner(opts.as(Config.class));
    }

    public interface Config extends PipelineOptions {

        @Validation.Required
        String getGraphOutput();

        void setGraphOutput(String output);

        @Default.Boolean(true)
        boolean getGraphAdjust();

        void setGraphAdjust(boolean value);

        @Default.Boolean(false)
        boolean getGraphShow();

        void setGraphShow(boolean value);

        @Default.Integer(-1)
        int getGraphWidth();

        void setGraphWidth(int value);

        @Default.Integer(-1)
        int getGraphHeight();

        void setGraphHeight(int value);

        class Register implements PipelineOptionsRegistrar {

            @Override
            public Iterable<Class<? extends PipelineOptions>> getPipelineOptions() {
                return Collections.singleton(Config.class);
            }
        }
    }

    public static class GraphResult implements PipelineResult {

        private final Diagram diagram;

        private GraphResult(final Diagram diagram) {
            this.diagram = diagram;
        }

        public Diagram getDiagram() {
            return diagram;
        }

        @Override
        public State getState() {
            return State.DONE;
        }

        @Override
        public State cancel() throws IOException {
            return State.CANCELLED;
        }

        @Override
        public State waitUntilFinish(final Duration duration) {
            return getState();
        }

        @Override
        public State waitUntilFinish() {
            return getState();
        }

        @Override
        public MetricResults metrics() {
            return new MetricResults() {

                @Override
                public MetricQueryResults queryMetrics(MetricsFilter filter) {
                    return new MetricQueryResults() {

                        @Override
                        public Iterable<MetricResult<Long>> counters() {
                            return emptyList();
                        }

                        @Override
                        public Iterable<MetricResult<DistributionResult>> distributions() {
                            return emptyList();
                        }

                        @Override
                        public Iterable<MetricResult<GaugeResult>> gauges() {
                            return emptyList();
                        }
                    };
                }
            };
        }
    }

    private static class CloseWindowWaiter extends WindowAdapter {

        private CountDownLatch latch;

        private CloseWindowWaiter(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void windowClosed(final WindowEvent e) {
            latch.countDown();
        }
    }
}
