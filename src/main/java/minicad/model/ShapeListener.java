package minicad.model;

/** Observer of a single shape. */
@FunctionalInterface
public interface ShapeListener {

    void shapeChanged(Shape shape);
}
