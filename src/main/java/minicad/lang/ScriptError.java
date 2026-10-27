package minicad.lang;

import minicad.command.CommandException;

/**
 * A mistake in a command line. It knows the columns it refers to, so the console can underline
 * them, and the usage of the command, to show as a hint.
 */
public final class ScriptError extends CommandException {

    private final Span span;
    private final String hint;
    private final String source;

    public ScriptError(String message, Span span) {
        this(message, span, null, null);
    }

    public ScriptError(String message, Span span, String hint, String source) {
        super(message);
        this.span = span;
        this.hint = hint;
        this.source = source;
    }

    /** The columns of the mistake, or null if it is not tied to a part of the line. */
    public Span span() {
        return span;
    }

    /** How the command is written, or null. */
    public String hint() {
        return hint;
    }

    /** The line with the mistake, when it is not the line the user typed (a line of a file). */
    public String source() {
        return source;
    }

    ScriptError withHint(String hint) {
        return this.hint != null ? this : new ScriptError(getMessage(), span, hint, source);
    }

    /** The same mistake, placed in a line of a file. */
    public ScriptError at(String where, String line) {
        return new ScriptError(where + ": " + getMessage(), span, hint, line);
    }
}
