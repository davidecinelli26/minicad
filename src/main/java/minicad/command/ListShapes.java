package minicad.command;

import minicad.io.Describer;
import minicad.model.Shape;

import java.util.List;

/** {@code ls}: describes shapes, one per line, groups with their members indented. */
public record ListShapes(List<Shape> shapes, String whenEmpty) implements Query {

    @Override
    public void execute(Workspace workspace) {
        if (shapes.isEmpty()) {
            workspace.print(whenEmpty);
        }
        shapes.stream().flatMap(shape -> Describer.describe(shape).stream()).forEach(workspace::print);
    }
}
