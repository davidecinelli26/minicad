package minicad.command;

import minicad.model.Shape;

/** {@code new}: adds a shape and prints the id it got. */
public record Create(Shape shape) implements Command {

    @Override
    public void execute(Workspace workspace) {
        workspace.drawing().add(shape);
        workspace.print(shape.id());
    }
}
