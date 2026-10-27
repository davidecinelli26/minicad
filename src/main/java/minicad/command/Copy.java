package minicad.command;

import minicad.model.Point;
import minicad.model.Shape;

/**
 * {@code copy}: duplicates a shape, a whole group included, shifted by an offset (Prototype: the
 * shape copies itself, so this command needs no knowledge of what kind of shape it is).
 */
public record Copy(Shape original, Point offset) implements Command {

    @Override
    public void execute(Workspace workspace) {
        Shape copy = original.copy();
        copy.moveBy(offset.x(), offset.y());
        workspace.drawing().add(copy);
        workspace.print(copy.id());
    }
}
