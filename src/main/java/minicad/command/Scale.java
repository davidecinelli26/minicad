package minicad.command;

import minicad.model.Shape;

/** {@code scale}: resizes a shape about its centre; a group about the centre of the group. */
public record Scale(Shape shape, double factor) implements Command {

    @Override
    public void execute(Workspace workspace) {
        shape.scale(factor);
    }
}
