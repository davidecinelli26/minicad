package minicad.ui;

import minicad.model.Numbers;
import minicad.model.Point;
import minicad.model.Shape;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** Drag to draw: a circle from its centre, or a rectangle from corner to corner (shift: a square). */
abstract sealed class ShapeTool implements Tool {

    final Context context;
    Point start;
    Point end;
    boolean square;

    ShapeTool(Context context) {
        this.context = context;
    }

    @Override
    public Cursor cursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void pressed(Pointer pointer) {
        start = context.snap(pointer.at());
        end = start;
    }

    @Override
    public void dragged(Pointer pointer) {
        if (start != null) {
            end = context.snap(pointer.at());
            square = pointer.shift();
            context.repaint();
        }
    }

    @Override
    public void released(Pointer pointer) {
        if (start == null) {
            return;
        }
        end = context.snap(pointer.at());
        square = pointer.shift();
        String command = command();
        start = end = null;
        if (command != null && context.run(command)) {
            List<Shape> items = context.drawing().items();
            context.selection().set(items.getLast().id());
        }
        context.repaint();
    }

    /** The command that creates the shape, or null if the drag was too short to mean one. */
    abstract String command();

    static Rectangle2D box(Point a, Point b) {
        return new Rectangle2D.Double(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()),
                Math.abs(a.x() - b.x()), Math.abs(a.y() - b.y()));
    }

    static void dashed(Graphics2D g) {
        g.setColor(Theme.ACCENT);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{5, 4}, 0));
    }

    static final class CircleTool extends ShapeTool {

        CircleTool(Context context) {
            super(context);
        }

        @Override
        public String name() {
            return "Circle";
        }

        private double radius() {
            return start.distance(end);
        }

        @Override
        String command() {
            if (radius() < 1) {
                return null;
            }
            return "new circle (" + Numbers.exact(radius()) + ") " + start;
        }

        @Override
        public void paintPreview(Graphics2D g) {
            if (start == null) {
                return;
            }
            double r = radius();
            dashed(g);
            g.draw(new Ellipse2D.Double(start.x() - r, start.y() - r, 2 * r, 2 * r));
            g.draw(new Line2D.Double(start.x(), start.y(), end.x(), end.y()));
        }
    }

    static final class RectangleTool extends ShapeTool {

        RectangleTool(Context context) {
            super(context);
        }

        @Override
        public String name() {
            return "Rectangle";
        }

        /** The far corner, moved to make a square when shift is held. */
        private Point corner() {
            if (!square) {
                return end;
            }
            double side = Math.max(Math.abs(end.x() - start.x()), Math.abs(end.y() - start.y()));
            return new Point(start.x() + Math.copySign(side, end.x() - start.x()),
                    start.y() + Math.copySign(side, end.y() - start.y()));
        }

        @Override
        String command() {
            Rectangle2D box = box(start, corner());
            if (box.getWidth() < 1 || box.getHeight() < 1) {
                return null;
            }
            return "new rectangle " + new Point(box.getCenterX(), box.getCenterY())
                    + " (" + Numbers.exact(box.getWidth()) + "," + Numbers.exact(box.getHeight()) + ")";
        }

        @Override
        public void paintPreview(Graphics2D g) {
            if (start != null) {
                dashed(g);
                g.draw(box(start, corner()));
            }
        }
    }
}
