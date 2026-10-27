package minicad.model;

/** Observer of a whole drawing: the canvas and the object tree. */
@FunctionalInterface
public interface DrawingListener {

    enum Change {
        /** A shape moved, was resized or recoloured. */
        SHAPE,
        /** Shapes were added, removed or regrouped, or the whole drawing was replaced. */
        STRUCTURE
    }

    void drawingChanged(Change change);
}
