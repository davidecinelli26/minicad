package minicad.model;

/**
 * The Visitor: an operation over shapes written in one class instead of spread over the shapes.
 * The screen renderer, the SVG exporter, the script writer and the {@code ls} listing are visitors,
 * so the model knows nothing of Swing, SVG or files. The price is the classic one: a new kind of
 * shape means a new method here and in every visitor.
 */
public interface ShapeVisitor<R> {

    R visitCircle(Circle circle);

    R visitRectangle(Rectangle rectangle);

    R visitPicture(Picture picture);

    R visitGroup(Group group);
}
