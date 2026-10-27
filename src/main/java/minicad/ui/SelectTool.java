package minicad.ui;

import minicad.io.ScriptWriter;
import minicad.model.Point;
import minicad.model.Shape;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Optional;

/** Click to select, shift-click to add, drag to move: the drag becomes one {@code mvoff}. */
final class SelectTool implements Tool {

    private final Context context;
    private Point anchor;
    private double dx;
    private double dy;

    SelectTool(Context context) {
        this.context = context;
    }

    @Override
    public String name() {
        return "Select";
    }

    @Override
    public void pressed(Pointer pointer) {
        anchor = null;
        dx = dy = 0;
        Selection selection = context.selection();
        // Pressing on a selected shape, a member of a group included, drags the selection as it is.
        boolean onSelection = selection.shapes(context.drawing()).stream().anyMatch(s -> s.contains(pointer.at()));
        Optional<Shape> hit = topmostAt(pointer.at());
        if (pointer.shift()) {
            hit.ifPresent(shape -> selection.toggle(shape.id()));
            return;
        }
        if (!onSelection) {
            if (hit.isEmpty()) {
                selection.clear();
                return;
            }
            selection.set(hit.get().id());
        }
        anchor = context.snap(pointer.at());
    }

    @Override
    public void dragged(Pointer pointer) {
        if (anchor != null) {
            Point to = context.snap(pointer.at());
            dx = to.x() - anchor.x();
            dy = to.y() - anchor.y();
            context.repaint();
        }
    }

    @Override
    public void released(Pointer pointer) {
        if (anchor != null && (dx != 0 || dy != 0)) {
            List<Shape> moving = context.selection().shapes(context.drawing());
            context.run("mvoff " + ScriptWriter.ids(moving) + " " + new Point(dx, dy));
        }
        anchor = null;
        dx = dy = 0;
        context.repaint();
    }

    @Override
    public void paintPreview(Graphics2D g) {
        if (anchor == null || (dx == 0 && dy == 0)) {
            return;
        }
        Graphics2D ghost = (Graphics2D) g.create();
        ghost.translate(dx, dy);
        ghost.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        Java2DRenderer renderer = new Java2DRenderer(ghost);
        context.selection().shapes(context.drawing()).forEach(shape -> shape.accept(renderer));
        ghost.dispose();
    }

    /** The topmost shape under the point, at the top level: clicking a member selects its group. */
    private Optional<Shape> topmostAt(Point p) {
        List<Shape> items = context.drawing().items();
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).contains(p)) {
                return Optional.of(items.get(i));
            }
        }
        return Optional.empty();
    }
}
