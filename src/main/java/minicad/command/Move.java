package minicad.command;

import minicad.model.Point;
import minicad.model.Shape;

import java.util.List;

/**
 * {@code mv} puts the centre of a shape at a point; {@code mvoff} moves shapes by an offset. A group
 * is translated as a whole, so its members keep their layout.
 */
public record Move(List<Shape> shapes, Point point, boolean relative) implements Command {

    public Move {
        if (!relative && shapes.size() != 1) {
            throw new IllegalArgumentException("mv moves one shape to a point; mvoff moves several");
        }
    }

    @Override
    public void execute(Workspace workspace) {
        for (Shape shape : shapes) {
            if (relative) {
                shape.moveBy(point.x(), point.y());
            } else {
                shape.moveTo(point);
            }
        }
    }
}
