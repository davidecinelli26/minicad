package minicad.lang;

/** A word, number, string, colour or punctuation mark of a command line. */
public record Token(Type type, String text, Span span) {

    public enum Type {
        /** A keyword, an id or a bare path: starts with a letter. */
        WORD("a word"),
        NUMBER("a number"),
        /** A quoted string; {@link #text()} has the quotes and escapes removed. */
        STRING("a quoted string"),
        /** {@code #rgb} or {@code #rrggbb}. */
        COLOR("a colour"),
        LPAREN("'('"),
        RPAREN("')'"),
        COMMA("','"),
        END("the end of the line");

        private final String description;

        Type(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    public boolean is(Type type) {
        return this.type == type;
    }

    /** Whether this is the given keyword, in any case. */
    public boolean isWord(String word) {
        return type == Type.WORD && text.equalsIgnoreCase(word);
    }

    /** How to name this token in an error message. */
    public String describe() {
        return switch (type) {
            case END -> type.description();
            case STRING -> "\"" + text + "\"";
            default -> "'" + text + "'";
        };
    }
}
