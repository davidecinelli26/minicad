package minicad.ui;

import minicad.model.Drawing;
import minicad.model.Point;

import java.awt.Cursor;
import java.awt.Graphics2D;

/**
 * What the mouse does on the canvas: the State pattern. The canvas hands every press, drag and
 * release to the current tool, and changing tool changes its behaviour without a single
 * {@code if (mode == ...)} in the canvas.
 * <p>
 * A tool never edits the drawing itself: when a gesture ends it writes the command line that does
 * it, as if typed in the console. The line shows up in the console, and goes into the same undo
 * history as everything else.
 */
interface Tool {

    /** A mouse position: in drawing units, and in pixels on screen. */
    record Pointer(Point at, double screenX, double screenY, boolean shift) {
    }

    /** What the canvas offers to its tools. */
    interface Context {

        Drawing drawing();

        Selection selection();

        /** Runs a command line as if typed; returns whether it worked. */
        boolean run(String command);

        /** The point on the grid nearest to {@code p} when snapping is on; {@code p} itself otherwise. */
        Point snap(Point p);

        /** Moves the view by pixels on screen. */
        void panBy(double dx, double dy);

        void repaint();
    }

    String name();

    default Cursor cursor() {
        return Cursor.getDefaultCursor();
    }

    default void pressed(Pointer pointer) {
    }

    default void dragged(Pointer pointer) {
    }

    default void released(Pointer pointer) {
    }

    /** Draws the gesture in progress, in drawing units, over the shapes. */
    default void paintPreview(Graphics2D g) {
    }
}
