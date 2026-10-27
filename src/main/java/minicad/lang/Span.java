package minicad.lang;

/** Columns {@code start} (inclusive) to {@code end} (exclusive) of a command line, from 0. */
public record Span(int start, int end) {

    public Span {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("bad span " + start + ".." + end);
        }
    }

    /** The span that covers both. */
    public Span to(Span other) {
        return new Span(Math.min(start, other.start), Math.max(end, other.end));
    }
}
