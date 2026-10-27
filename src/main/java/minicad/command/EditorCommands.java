package minicad.command;

/** {@code undo}, {@code redo} and {@code clear}. */
public final class EditorCommands {

    private EditorCommands() {
    }

    /** Not an edit itself: it moves along the history instead of adding to it. */
    public static Query undo() {
        return workspace -> workspace.print(workspace.history().undo(workspace.drawing())
                .map(label -> "undone: " + label)
                .orElse("nothing to undo"));
    }

    public static Query redo() {
        return workspace -> workspace.print(workspace.history().redo(workspace.drawing())
                .map(label -> "redone: " + label)
                .orElse("nothing to redo"));
    }

    /** Empties the drawing; ids start again from obj0. Undo brings everything back. */
    public static Command clear() {
        return workspace -> workspace.drawing().clear();
    }
}
