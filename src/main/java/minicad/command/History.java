package minicad.command;

import minicad.model.Drawing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Runs commands and undoes them. It is the Invoker of the Command pattern and the Caretaker of the
 * Memento: before each edit it keeps a snapshot of the drawing, and undo puts the snapshot back.
 * <p>
 * No command has to know how to undo itself. In the 2024 version every command had a hand-written
 * inverse, and some were wrong: scaling back by {@code 1/factor} drifted, and undoing a move of a
 * group could not restore the members. A snapshot also makes every edit atomic: if a command fails
 * halfway, for instance on line 7 of a script being loaded, the drawing is restored as it was.
 */
public final class History {

    public static final int DEFAULT_LIMIT = 100;

    /** A step of the history: the command line, and the drawing on the other side of it. */
    private record Step(String label, Drawing.Memento state) {
    }

    private final Deque<Step> undo = new ArrayDeque<>();
    private final Deque<Step> redo = new ArrayDeque<>();
    private final int limit;

    public History() {
        this(DEFAULT_LIMIT);
    }

    public History(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("the history must keep at least one step");
        }
        this.limit = limit;
    }

    /** Runs a command; if it edits the drawing, it can then be undone. */
    public void run(Command command, String label, Workspace workspace) {
        if (!command.isEdit()) {
            command.execute(workspace);
            return;
        }
        Drawing drawing = workspace.drawing();
        Drawing.Memento before = drawing.save();
        try {
            command.execute(workspace);
        } catch (RuntimeException e) {
            drawing.restore(before);
            throw e;
        }
        undo.push(new Step(label, before));
        if (undo.size() > limit) {
            undo.removeLast();
        }
        redo.clear();
    }

    /** Undoes the last edit and returns its label, or nothing if there was none. */
    public Optional<String> undo(Drawing drawing) {
        return travel(undo, redo, drawing);
    }

    /** Redoes the last undone edit and returns its label, or nothing if there was none. */
    public Optional<String> redo(Drawing drawing) {
        return travel(redo, undo, drawing);
    }

    private static Optional<String> travel(Deque<Step> from, Deque<Step> to, Drawing drawing) {
        Step step = from.poll();
        if (step == null) {
            return Optional.empty();
        }
        to.push(new Step(step.label(), drawing.save()));
        drawing.restore(step.state());
        return Optional.of(step.label());
    }

    public Optional<String> nextUndo() {
        return Optional.ofNullable(undo.peek()).map(Step::label);
    }

    public Optional<String> nextRedo() {
        return Optional.ofNullable(redo.peek()).map(Step::label);
    }

    public void clear() {
        undo.clear();
        redo.clear();
    }
}
