package minicad.app;

import minicad.lang.Span;

/**
 * An error to show the user: the line, the columns at fault, the message and how the command is
 * written. {@code span} and {@code hint} may be null.
 */
public record Diagnostic(String line, Span span, String message, String hint) {

    /**
     * As text, with the faulty columns marked:
     * <pre>
     * mv obj9 (10,10)
     *    ^^^^ there is no shape obj9
     * </pre>
     */
    public String render() {
        StringBuilder text = new StringBuilder();
        if (span != null && line != null) {
            text.append(line).append('\n')
                    .append(" ".repeat(span.start()))
                    .append("^".repeat(Math.max(1, span.end() - span.start())))
                    .append(' ').append(message);
        } else {
            text.append("error: ").append(message);
        }
        if (hint != null) {
            text.append('\n').append("usage: ").append(hint);
        }
        return text.toString();
    }
}
