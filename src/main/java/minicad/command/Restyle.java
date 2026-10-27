package minicad.command;

import minicad.model.Shape;
import minicad.model.Style;

import java.util.List;
import java.util.function.UnaryOperator;

/** {@code color}: changes fill and outline; on a group, of every figure in it. */
public record Restyle(List<Shape> shapes, UnaryOperator<Style> change) implements Command {

    @Override
    public void execute(Workspace workspace) {
        shapes.forEach(shape -> shape.restyle(change));
    }
}
