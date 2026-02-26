package fractal3d;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

public final class FractalViewerApp {
    private final Camera camera = new Camera();
    private final FractalRenderer renderer = new FractalRenderer();
    private final Set<Integer> cancelToken = new HashSet<>();

    private RenderPanel renderPanel;
    private JComboBox<FractalRenderer.SetType> setType;
    private JSpinner power;
    private JSpinner iterations;
    private JSpinner bailout;
    private JSpinner epsilon;
    private JSpinner raySteps;
    private JSpinner scale;
    private JComboBox<String> unitX;
    private JComboBox<String> unitY;
    private JComboBox<String> unitZ;
    private JSpinner juliaReal;
    private JSpinner juliaU1;
    private JSpinner juliaU2;
    private JSpinner juliaU3;

    private SwingWorker<BufferedImage, Void> worker;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FractalViewerApp().start());
    }

    private void start() {
        JFrame frame = new JFrame("Tricomplex Mandelbrot/Julia 3D Explorer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        renderPanel = new RenderPanel();
        renderPanel.setPreferredSize(new Dimension(960, 720));
        renderPanel.setBorder(BorderFactory.createEtchedBorder());
        installMouseControls();

        frame.add(renderPanel, BorderLayout.CENTER);
        frame.add(buildControls(), BorderLayout.NORTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        rerender();
    }

    private JPanel buildControls() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));

        setType = new JComboBox<>(FractalRenderer.SetType.values());
        power = new JSpinner(new SpinnerNumberModel(2, 2, 8, 1));
        iterations = new JSpinner(new SpinnerNumberModel(15, 5, 80, 1));
        bailout = new JSpinner(new SpinnerNumberModel(8.0, 2.0, 100.0, 0.5));
        epsilon = new JSpinner(new SpinnerNumberModel(0.03, 0.005, 0.2, 0.005));
        raySteps = new JSpinner(new SpinnerNumberModel(260, 50, 1000, 10));
        scale = new JSpinner(new SpinnerNumberModel(1.4, 0.2, 10.0, 0.1));

        String[] basis = FractalRenderer.basisLabels();
        unitX = new JComboBox<>(basis);
        unitY = new JComboBox<>(basis);
        unitZ = new JComboBox<>(basis);
        unitX.setSelectedIndex(1);
        unitY.setSelectedIndex(2);
        unitZ.setSelectedIndex(4);

        juliaReal = new JSpinner(new SpinnerNumberModel(0.0, -2.0, 2.0, 0.01));
        juliaU1 = new JSpinner(new SpinnerNumberModel(-0.2, -2.0, 2.0, 0.01));
        juliaU2 = new JSpinner(new SpinnerNumberModel(0.6, -2.0, 2.0, 0.01));
        juliaU3 = new JSpinner(new SpinnerNumberModel(0.2, -2.0, 2.0, 0.01));

        JButton renderButton = new JButton("Render");
        renderButton.addActionListener(e -> rerender());

        row.add(new JLabel("Set"));
        row.add(setType);
        row.add(new JLabel("p"));
        row.add(power);
        row.add(new JLabel("Iter"));
        row.add(iterations);
        row.add(new JLabel("Bailout"));
        row.add(bailout);
        row.add(new JLabel("ε"));
        row.add(epsilon);
        row.add(new JLabel("Ray steps"));
        row.add(raySteps);
        row.add(new JLabel("Scale"));
        row.add(scale);

        row.add(new JLabel("X/Y/Z units"));
        row.add(unitX);
        row.add(unitY);
        row.add(unitZ);

        row.add(new JLabel("Julia c: r/u1/u2/u3"));
        row.add(juliaReal);
        row.add(juliaU1);
        row.add(juliaU2);
        row.add(juliaU3);

        row.add(renderButton);
        return row;
    }

    private void installMouseControls() {
        MouseAdapter mouse = new MouseAdapter() {
            private int lastX;
            private int lastY;
            private boolean rotate = false;
            private boolean pan = false;

            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                rotate = SwingUtilities.isLeftMouseButton(e);
                pan = SwingUtilities.isRightMouseButton(e) || e.isShiftDown();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();

                if (rotate) {
                    camera.orbit(dx * 0.01, -dy * 0.01);
                    rerender();
                } else if (pan) {
                    camera.pan(-dx * 0.005, dy * 0.005);
                    rerender();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double factor = e.getWheelRotation() > 0 ? 1.1 : 0.9;
                camera.zoom(factor);
                rerender();
            }
        };
        renderPanel.addMouseListener(mouse);
        renderPanel.addMouseMotionListener(mouse);
        renderPanel.addMouseWheelListener(mouse);
    }

    private void rerender() {
        if (!validateUnits()) {
            JOptionPane.showMessageDialog(renderPanel, "Choisissez 3 unités distinctes pour la coupe 3D.");
            return;
        }

        if (worker != null && !worker.isDone()) {
            cancelToken.add(1);
            worker.cancel(true);
        }
        cancelToken.clear();

        FractalRenderer.Params params = new FractalRenderer.Params(
                (FractalRenderer.SetType) setType.getSelectedItem(),
                (int) power.getValue(),
                (int) iterations.getValue(),
                (double) bailout.getValue(),
                (double) epsilon.getValue(),
                (int) raySteps.getValue(),
                (double) scale.getValue(),
                unitX.getSelectedIndex(),
                unitY.getSelectedIndex(),
                unitZ.getSelectedIndex(),
                buildJuliaConstant()
        );

        renderPanel.setImage(null);
        worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() {
                return renderer.render(renderPanel.getWidth(), renderPanel.getHeight(), camera, params, cancelToken);
            }

            @Override
            protected void done() {
                if (!isCancelled()) {
                    try {
                        renderPanel.setImage(get());
                    } catch (Exception ignored) {
                        // Ignore canceled tasks.
                    }
                }
            }
        };
        worker.execute();
    }

    private boolean validateUnits() {
        int ux = unitX.getSelectedIndex();
        int uy = unitY.getSelectedIndex();
        int uz = unitZ.getSelectedIndex();
        return ux != uy && ux != uz && uy != uz;
    }

    private Tricomplex buildJuliaConstant() {
        double[] c = new double[8];
        c[0] = (double) juliaReal.getValue();
        c[unitX.getSelectedIndex()] = (double) juliaU1.getValue();
        c[unitY.getSelectedIndex()] = (double) juliaU2.getValue();
        c[unitZ.getSelectedIndex()] = (double) juliaU3.getValue();
        return new Tricomplex(c);
    }

    private static final class RenderPanel extends JPanel {
        private BufferedImage image;

        void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (image == null) {
                g2.drawString("Rendering...", 20, 20);
                return;
            }
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        }
    }
}
