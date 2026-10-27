package minicad.command;

import minicad.model.Numbers;
import minicad.model.Shape;

import java.util.List;
import java.util.function.ToDoubleFunction;

/** {@code area} and {@code perimeter}: one line per shape, and the total when there are several. */
public record Measure(Quantity quantity, List<Shape> shapes, String whenEmpty) implements Query {

    public enum Quantity {
        AREA(Shape::area), PERIMETER(Shape::perimeter);

        private final ToDoubleFunction<Shape> measure;

        Quantity(ToDoubleFunction<Shape> measure) {
            this.measure = measure;
        }

        public double of(Shape shape) {
            return measure.applyAsDouble(shape);
        }
    }

    @Override
    public void execute(Workspace workspace) {
        if (shapes.isEmpty()) {
            workspace.print(whenEmpty);
            return;
        }
        int width = shapes.stream().mapToInt(shape -> shape.id().length()).max().orElse(0);
        double total = 0;
        for (Shape shape : shapes) {
            double value = quantity.of(shape);
            total += value;
            workspace.print(pad(shape.id(), width) + "  " + Numbers.brief(value));
        }
        if (shapes.size() > 1) {
            workspace.print(pad("total", width) + "  " + Numbers.brief(total));
        }
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }
}
