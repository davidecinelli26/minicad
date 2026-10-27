package minicad.lang;

import minicad.command.Workspace;
import minicad.lang.Terminal.Ref;
import minicad.model.Shape;

import java.util.List;

/** What {@code ls}, {@code area} and {@code perimeter} apply to: some ids, a kind, or everything. */
public sealed interface Target extends Expression<List<Shape>> {

    /** What to print when there is nothing to show. */
    String whenEmpty();

    /** {@code all}: the shapes at the top level; a group counts as one, with its members inside. */
    record All(Span span) implements Target {

        @Override
        public List<Shape> interpret(Workspace workspace) {
            return workspace.drawing().items();
        }

        @Override
        public String whenEmpty() {
            return "the drawing is empty";
        }
    }

    /** {@code circle}, {@code rectangle}, {@code image} or {@code groups}: at any depth. */
    record OfKind(String kind, Span span) implements Target {

        @Override
        public List<Shape> interpret(Workspace workspace) {
            return workspace.drawing().shapes().filter(shape -> shape.kind().equals(kind)).toList();
        }

        @Override
        public String whenEmpty() {
            return "there are no " + kind + "s";
        }
    }

    record Ids(List<Ref> refs, Span span) implements Target {

        @Override
        public List<Shape> interpret(Workspace workspace) {
            return Ref.all(refs, workspace);
        }

        @Override
        public String whenEmpty() {
            return "";
        }
    }
}
