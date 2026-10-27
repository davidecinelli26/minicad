package minicad.ui;

import java.awt.Cursor;

/** Drag to move the view; the drawing does not change. */
final class PanTool implements Tool {

    private final Context context;
    private double lastX;
    private double lastY;

    PanTool(Context context) {
        this.context = context;
    }

    @Override
    public String name() {
        return "Pan";
    }

    @Override
    public Cursor cursor() {
        return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }

    @Override
    public void pressed(Pointer pointer) {
        lastX = pointer.screenX();
        lastY = pointer.screenY();
    }

    @Override
    public void dragged(Pointer pointer) {
        context.panBy(pointer.screenX() - lastX, pointer.screenY() - lastY);
        lastX = pointer.screenX();
        lastY = pointer.screenY();
    }
}
