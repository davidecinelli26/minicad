package minicad.lang;

import minicad.command.Workspace;
import minicad.model.Group;
import minicad.model.Point;
import minicad.model.Rgb;
import minicad.model.Shape;
import minicad.model.Style;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/** The terminal expressions of the language: the values commands work on. */
public final class Terminal {

    private Terminal() {
    }

    public record Num(double value, Span span) implements Expression<Double> {

        @Override
        public Double interpret(Workspace workspace) {
            return value;
        }

        /** The value, checked to be above zero. */
        public double positive(String what) {
            if (!(value > 0)) {
                throw new ScriptError(what + " must be greater than 0", span);
            }
            return value;
        }
    }

    /** {@code (x,y)}: a position, an offset or a size. */
    public record Pt(Num x, Num y, Span span) implements Expression<Point> {

        @Override
        public Point interpret(Workspace workspace) {
            return new Point(x.value(), y.value());
        }
    }

    /** An id, looked up in the drawing. */
    public record Ref(String id, Span span) implements Expression<Shape> {

        @Override
        public Shape interpret(Workspace workspace) {
            return workspace.drawing().find(id).orElseThrow(() -> new ScriptError(
                    "there is no shape " + id + (workspace.drawing().isEmpty() ? ": the drawing is empty" : ""), span));
        }

        public Group group(Workspace workspace) {
            if (interpret(workspace) instanceof Group group) {
                return group;
            }
            throw new ScriptError(id + " is not a group", span);
        }

        static List<Shape> all(List<Ref> refs, Workspace workspace) {
            return refs.stream().map(ref -> ref.interpret(workspace)).toList();
        }
    }

    /** A file name, resolved against the working folder. */
    public record FilePath(String path, Span span) implements Expression<Path> {

        @Override
        public Path interpret(Workspace workspace) {
            try {
                return workspace.resolve(path);
            } catch (RuntimeException e) {
                throw new ScriptError("not a valid file name: " + path, span);
            }
        }

        /** The path, required to end with the given extension. */
        public Path withExtension(String extension, Workspace workspace) {
            if (!path.toLowerCase(Locale.ROOT).endsWith(extension)) {
                throw new ScriptError("the file name should end with " + extension, span);
            }
            return interpret(workspace);
        }
    }

    /** {@code fill #rrggbb}, {@code stroke none}: a change to the style. */
    public record Paint(boolean fill, Rgb colour, Span span) implements Expression<UnaryOperator<Style>> {

        @Override
        public UnaryOperator<Style> interpret(Workspace workspace) {
            return fill ? style -> style.withFill(colour) : style -> style.withStroke(colour);
        }

        static UnaryOperator<Style> all(List<Paint> paints, Workspace workspace) {
            UnaryOperator<Style> change = UnaryOperator.identity();
            for (Paint paint : paints) {
                UnaryOperator<Style> before = change, next = paint.interpret(workspace);
                change = style -> next.apply(before.apply(style));
            }
            return change;
        }
    }
}
