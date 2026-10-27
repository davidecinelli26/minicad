package minicad.ui;

import minicad.model.Bounds;
import minicad.model.Drawing;
import minicad.model.DrawingListener;
import minicad.model.Point;
import minicad.model.Shape;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The view of the drawing (MVC). It observes the drawing and the selection and repaints when they
 * change; it hands the mouse to the current {@link Tool}. The wheel zooms at the cursor and the
 * middle button pans, whatever the tool.
 */
final class Canvas extends JComponent implements DrawingListener {

    static final double GRID = 10;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 20;

    private final Drawing drawing;
    private final Selection selection;
    private Tool tool;
    private double zoom = 1;
    private double offsetX = 40;
    private double offsetY = 40;
    private boolean snap = true;
    private Point cursor;
    private Shape hovered;
    private Consumer<Canvas> onViewChange = canvas -> { };

    Canvas(Drawing drawing, Selection selection) {
        this.drawing = drawing;
        this.selection = selection;
        drawing.addListener(this);
        selection.addListener(this::repaint);
        setFocusable(true);
        setOpaque(true);
        MouseAdapter mouse = new MouseAdapter() {
            private boolean panning;
            private double lastX;
            private double lastY;

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    panning = true;
                    lastX = e.getX();
                    lastY = e.getY();
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    tool.pressed(pointer(e));
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                track(e);
                if (panning) {
                    panBy(e.getX() - lastX, e.getY() - lastY);
                    lastX = e.getX();
                    lastY = e.getY();
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    tool.dragged(pointer(e));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (panning) {
                    panning = false;
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    tool.released(pointer(e));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                track(e);
                Shape over = topmostAt(cursor).orElse(null);
                if (over != hovered) {
                    hovered = over;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cursor = null;
                hovered = null;
                repaint();
                onViewChange.accept(Canvas.this);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomAt(Math.pow(1.1, -e.getPreciseWheelRotation()), e.getX(), e.getY());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    @Override
    public void drawingChanged(Change change) {
        if (change == Change.STRUCTURE) {
            selection.prune(drawing);
            if (hovered != null && drawing.find(hovered.id()).orElse(null) != hovered) {
                hovered = null;
            }
        }
        repaint();
    }

    void setTool(Tool tool) {
        this.tool = tool;
        setCursor(tool.cursor());
        repaint();
    }

    Tool tool() {
        return tool;
    }

    void onViewChange(Consumer<Canvas> listener) {
        onViewChange = listener;
    }

    double zoom() {
        return zoom;
    }

    /** The cursor in drawing units, or null if the mouse is elsewhere. */
    Point cursor() {
        return cursor;
    }

    boolean isSnapping() {
        return snap;
    }

    void setSnapping(boolean snap) {
        this.snap = snap;
        onViewChange.accept(this);
    }

    Point snap(Point p) {
        return snap ? new Point(Math.round(p.x() / GRID) * GRID, Math.round(p.y() / GRID) * GRID) : p;
    }

    void panBy(double dx, double dy) {
        offsetX += dx;
        offsetY += dy;
        repaint();
    }

    void zoomAt(double factor, double screenX, double screenY) {
        double next = Math.clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
        double applied = next / zoom;
        offsetX = screenX - (screenX - offsetX) * applied;
        offsetY = screenY - (screenY - offsetY) * applied;
        zoom = next;
        repaint();
        onViewChange.accept(this);
    }

    void zoomBy(double factor) {
        zoomAt(factor, getWidth() / 2.0, getHeight() / 2.0);
    }

    /** Zooms to show the whole drawing, or resets the view if it is empty. */
    void fit() {
        Optional<Bounds> bounds = drawing.bounds();
        if (bounds.isEmpty() || getWidth() == 0) {
            zoom = 1;
            offsetX = offsetY = 40;
        } else {
            Bounds b = bounds.get().grow(30);
            zoom = Math.clamp(Math.min(getWidth() / b.width(), getHeight() / b.height()), MIN_ZOOM, MAX_ZOOM);
            offsetX = getWidth() / 2.0 - b.center().x() * zoom;
            offsetY = getHeight() / 2.0 - b.center().y() * zoom;
        }
        repaint();
        onViewChange.accept(this);
    }

    Point toDrawing(double screenX, double screenY) {
        return new Point((screenX - offsetX) / zoom, (screenY - offsetY) / zoom);
    }

    private Tool.Pointer pointer(MouseEvent e) {
        return new Tool.Pointer(toDrawing(e.getX(), e.getY()), e.getX(), e.getY(), e.isShiftDown());
    }

    private void track(MouseEvent e) {
        cursor = toDrawing(e.getX(), e.getY());
        onViewChange.accept(this);
    }

    private Optional<Shape> topmostAt(Point p) {
        if (p == null) {
            return Optional.empty();
        }
        List<Shape> items = drawing.items();
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).contains(p)) {
                return Optional.of(items.get(i));
            }
        }
        return Optional.empty();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Theme.PAPER);
        g.fillRect(0, 0, getWidth(), getHeight());
        paintGrid(g);

        Graphics2D world = (Graphics2D) g.create();
        world.translate(offsetX, offsetY);
        world.scale(zoom, zoom);
        Java2DRenderer renderer = new Java2DRenderer(world);
        drawing.items().forEach(shape -> shape.accept(renderer));
        tool.paintPreview(world);
        world.dispose();

        if (hovered != null && !selection.contains(hovered.id())) {
            outline(g, hovered.bounds(), new Color(0x2a9d8f, false), 1f, null);
        }
        for (Shape shape : selection.shapes(drawing)) {
            outline(g, shape.bounds(), Theme.ACCENT, 1.5f, shape.id());
        }
        if (drawing.isEmpty()) {
            paintHint(g);
        }
        g.dispose();
    }

    /** Grid lines every 10 units, or 50 or 100 when zoomed out; thicker every five, and the axes. */
    private void paintGrid(Graphics2D g) {
        double step = GRID;
        while (step * zoom < 8) {
            step *= 5;
        }
        Point topLeft = toDrawing(0, 0);
        Point bottomRight = toDrawing(getWidth(), getHeight());
        g.setStroke(new BasicStroke(1f));
        for (double x = Math.floor(topLeft.x() / step) * step; x <= bottomRight.x(); x += step) {
            double sx = Math.round(x * zoom + offsetX) + 0.5;
            g.setColor(colourOfLine(x, step));
            g.draw(new Line2D.Double(sx, 0, sx, getHeight()));
        }
        for (double y = Math.floor(topLeft.y() / step) * step; y <= bottomRight.y(); y += step) {
            double sy = Math.round(y * zoom + offsetY) + 0.5;
            g.setColor(colourOfLine(y, step));
            g.draw(new Line2D.Double(0, sy, getWidth(), sy));
        }
    }

    private static Color colourOfLine(double value, double step) {
        if (Math.abs(value) < step / 2) {
            return Theme.AXIS;
        }
        return Math.abs(Math.round(value / step)) % 5 == 0 ? Theme.GRID_MAJOR : Theme.GRID_MINOR;
    }

    /** A frame around a shape, in screen pixels so it stays thin at any zoom, with its id. */
    private void outline(Graphics2D g, Bounds b, Color colour, float width, String label) {
        Point2D a = new Point2D.Double(b.minX() * zoom + offsetX, b.minY() * zoom + offsetY);
        Rectangle2D box = new Rectangle2D.Double(a.getX() - 4, a.getY() - 4, b.width() * zoom + 8, b.height() * zoom + 8);
        g.setColor(colour);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1,
                label == null ? null : new float[]{4, 3}, 0));
        g.draw(box);
        if (label != null) {
            for (double[] corner : new double[][]{{box.getMinX(), box.getMinY()}, {box.getMaxX(), box.getMinY()},
                    {box.getMinX(), box.getMaxY()}, {box.getMaxX(), box.getMaxY()}}) {
                Rectangle2D handle = new Rectangle2D.Double(corner[0] - 3, corner[1] - 3, 6, 6);
                g.setColor(Color.WHITE);
                g.fill(handle);
                g.setColor(colour);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(handle);
            }
            g.setFont(Theme.mono(11f));
            FontMetrics metrics = g.getFontMetrics();
            int w = metrics.stringWidth(label) + 10;
            int h = metrics.getHeight() + 2;
            int x = (int) box.getMinX();
            int y = (int) box.getMinY() - h - 4;
            g.setColor(colour);
            g.fillRoundRect(x, y, w, h, 6, 6);
            g.setColor(Color.WHITE);
            g.drawString(label, x + 5, y + metrics.getAscent() + 1);
        }
    }

    private void paintHint(Graphics2D g) {
        String[] lines = {
                "Draw with the tools above, or type a command below:",
                "new circle (60) (200,150) fill #2a9d8f",
                "Help › Example drawing opens a finished one."
        };
        g.setFont(getFont().deriveFont(Font.PLAIN, 14f));
        FontMetrics plain = g.getFontMetrics();
        int y = getHeight() / 2 - plain.getHeight() * 2;
        for (int i = 0; i < lines.length; i++) {
            g.setFont(i == 1 ? Theme.mono(14f) : getFont().deriveFont(Font.PLAIN, 14f));
            g.setColor(i == 1 ? Theme.ACCENT : Theme.HINT);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(lines[i], (getWidth() - metrics.stringWidth(lines[i])) / 2, y);
            y += metrics.getHeight() + 6;
        }
    }
}
