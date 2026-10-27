package minicad.model;

import java.util.Objects;
import java.util.function.UnaryOperator;

/** A shape that is painted directly: the Leaf of the Composite, as opposed to a {@link Group}. */
public abstract class Figure extends Shape {

    private Style style;

    protected Figure(Style style) {
        this.style = Objects.requireNonNull(style);
    }

    public Style style() {
        return style;
    }

    @Override
    public void restyle(UnaryOperator<Style> change) {
        style = Objects.requireNonNull(change.apply(style));
        changed();
    }
}
