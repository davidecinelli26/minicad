package minicad.command;

/**
 * A request to the editor, as an object (Command). The interpreter turns every line into one, the
 * mouse tools produce the same lines, and {@link History} runs them all, which is what makes one
 * undo history for the console and the mouse.
 */
@FunctionalInterface
public interface Command {

    void execute(Workspace workspace);

    /** Whether the command edits the drawing, and so can be undone. */
    default boolean isEdit() {
        return true;
    }
}
