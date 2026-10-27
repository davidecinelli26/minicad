package minicad.command;

import minicad.model.Drawing;
import minicad.model.Shape;

import java.util.List;

/** {@code del}: removes shapes; deleting a group deletes its members. */
public record Delete(List<Shape> shapes) implements Command {

    @Override
    public void execute(Workspace workspace) {
        Drawing drawing = workspace.drawing();
        for (Shape shape : shapes) {
            // A shape may have gone already with a group listed before it.
            if (drawing.find(shape.id()).orElse(null) == shape) {
                drawing.remove(shape);
            }
        }
    }
}
