package minicad.command;

import minicad.model.Group;
import minicad.model.Shape;

import java.util.List;

/** {@code grp}: puts shapes into a new group and prints its id. */
public record GroupShapes(List<Shape> shapes) implements Command {

    @Override
    public void execute(Workspace workspace) {
        Group group = workspace.drawing().group(shapes);
        workspace.print(group.id());
    }
}
