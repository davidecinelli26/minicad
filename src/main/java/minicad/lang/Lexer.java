package minicad.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a command line into tokens. Unlike {@code split(" ")} in the 2024 version, spaces are free:
 * {@code ( 120 , 80 )} and {@code (120,80)} are the same, and paths with spaces can be quoted.
 * Everything after {@code //} is a comment.
 */
public final class Lexer {

    private final String line;
    private int pos;

    private Lexer(String line) {
        this.line = line;
    }

    public static List<Token> tokenize(String line) {
        return new Lexer(line).tokens();
    }

    private List<Token> tokens() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipBlanks();
            if (pos >= line.length() || line.startsWith("//", pos)) {
                tokens.add(new Token(Token.Type.END, "", new Span(line.length(), line.length())));
                return tokens;
            }
            tokens.add(next());
        }
    }

    private Token next() {
        int start = pos;
        char c = line.charAt(pos);
        return switch (c) {
            case '(' -> single(Token.Type.LPAREN);
            case ')' -> single(Token.Type.RPAREN);
            case ',' -> single(Token.Type.COMMA);
            case '"' -> string();
            case '#' -> colour();
            default -> {
                if (Character.isLetter(c) || c == '_') {
                    yield word();
                }
                if (Character.isDigit(c) || c == '.' || c == '-' || c == '+') {
                    yield number();
                }
                throw new ScriptError("unexpected character '" + c + "'", new Span(start, start + 1));
            }
        };
    }

    private Token single(Token.Type type) {
        pos++;
        return new Token(type, line.substring(pos - 1, pos), new Span(pos - 1, pos));
    }

    /** Letters, digits and the characters of a path, so that {@code save garden.mcad} needs no quotes. */
    private Token word() {
        int start = pos;
        while (pos < line.length() && (Character.isLetterOrDigit(line.charAt(pos)) || "_-./\\:".indexOf(line.charAt(pos)) >= 0)) {
            pos++;
        }
        return new Token(Token.Type.WORD, line.substring(start, pos), new Span(start, pos));
    }

    private Token number() {
        int start = pos;
        if (line.charAt(pos) == '-' || line.charAt(pos) == '+') {
            pos++;
        }
        int digits = 0;
        while (pos < line.length() && Character.isDigit(line.charAt(pos))) {
            pos++;
            digits++;
        }
        if (pos < line.length() && line.charAt(pos) == '.') {
            pos++;
            while (pos < line.length() && Character.isDigit(line.charAt(pos))) {
                pos++;
                digits++;
            }
        }
        Span span = new Span(start, pos);
        if (digits == 0) {
            throw new ScriptError("expected a number", span.end() == start ? new Span(start, start + 1) : span);
        }
        if (pos < line.length() && (Character.isLetter(line.charAt(pos)) || line.charAt(pos) == '_')) {
            throw new ScriptError("a number cannot be followed by a letter", new Span(start, pos + 1));
        }
        return new Token(Token.Type.NUMBER, line.substring(start, pos), span);
    }

    private Token string() {
        int start = pos++;
        StringBuilder text = new StringBuilder();
        while (pos < line.length() && line.charAt(pos) != '"') {
            char c = line.charAt(pos++);
            if (c == '\\' && pos < line.length() && (line.charAt(pos) == '"' || line.charAt(pos) == '\\')) {
                c = line.charAt(pos++);
            }
            text.append(c);
        }
        if (pos >= line.length()) {
            throw new ScriptError("this string has no closing quote", new Span(start, line.length()));
        }
        pos++;
        return new Token(Token.Type.STRING, text.toString(), new Span(start, pos));
    }

    private Token colour() {
        int start = pos++;
        while (pos < line.length() && Character.isLetterOrDigit(line.charAt(pos))) {
            pos++;
        }
        String text = line.substring(start, pos);
        Span span = new Span(start, pos);
        if (!text.matches("#(\\p{XDigit}{3}|\\p{XDigit}{6})")) {
            throw new ScriptError("a colour is # and 3 or 6 hex digits, like #2a9d8f", span);
        }
        return new Token(Token.Type.COLOR, text, span);
    }

    private void skipBlanks() {
        while (pos < line.length() && Character.isWhitespace(line.charAt(pos))) {
            pos++;
        }
    }
}
