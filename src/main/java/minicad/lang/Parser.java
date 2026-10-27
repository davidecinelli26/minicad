package minicad.lang;

import minicad.lang.Terminal.FilePath;
import minicad.lang.Terminal.Num;
import minicad.lang.Terminal.Paint;
import minicad.lang.Terminal.Pt;
import minicad.lang.Terminal.Ref;
import minicad.lang.Token.Type;
import minicad.model.Rgb;
import minicad.model.Shape;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the syntax tree of a command line by recursive descent, one method per rule:
 *
 * <pre>
 * statement ::= "new" shape paint*  | "copy" id point?    | "del" ids       | "mv" id point
 *             | "mvoff" ids point   | "scale" id number   | "grp" ids       | "ungrp" id
 *             | "color" ids (colour | paint+)             | ("ls" | "area" | "perimeter") target
 *             | ("save" | "load" | "export") path         | "undo" | "redo" | "clear" | "help" word?
 * shape     ::= "circle" radius point | "rectangle" point point | ("image" | "img") path point
 * radius    ::= number | "(" number ")"
 * point     ::= "(" number ","? number ")"
 * paint     ::= ("fill" | "stroke") (colour | "none")
 * target    ::= "all" | kind | ids
 * ids       ::= id ((",")? id)*
 * path      ::= string | word | "(" (string | word) ")"
 * </pre>
 */
public final class Parser {

    private static final Set<String> KINDS = Set.of("circle", "rectangle", "image", "group");

    private final List<Token> tokens;
    private int next;
    private Verb verb;

    private Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** The statement on a line, or nothing for a blank line or a comment. */
    public static Optional<Statement> parse(String line) {
        Parser parser = new Parser(Lexer.tokenize(line));
        if (parser.peek().is(Type.END)) {
            return Optional.empty();
        }
        try {
            Statement statement = parser.statement();
            Token extra = parser.peek();
            if (!extra.is(Type.END)) {
                throw parser.error("unexpected " + extra.describe() + " after the end of the command", extra.span());
            }
            return Optional.of(statement);
        } catch (ScriptError e) {
            throw parser.verb == null ? e : e.withHint(parser.verb.hint());
        }
    }

    private Statement statement() {
        Token word = peek();
        if (!word.is(Type.WORD)) {
            throw error("a command starts with its name, for example: new, mv, ls, help", word.span());
        }
        verb = Verb.of(word.text()).orElseThrow(() -> error("unknown command '" + word.text() + "'"
                + Verb.closest(word.text()).map(v -> ": did you mean " + v.keyword() + "?")
                .orElse(": type help for the list"), word.span()));
        take();
        Span start = word.span();
        return switch (verb) {
            case NEW -> create(start);
            case COPY -> {
                Ref shape = ref();
                Pt offset = peek().is(Type.LPAREN) ? point("offset") : null;
                yield new Statement.Copy(shape, offset, start.to(last()));
            }
            case DEL -> new Statement.Del(refs(), start.to(last()));
            case MV -> {
                Ref shape = ref();
                if (peek().is(Type.WORD)) {
                    throw error("mv moves one shape to a point; to move several by an offset use mvoff",
                            peek().span());
                }
                yield new Statement.Mv(shape, point("point"), start.to(last()));
            }
            case MVOFF -> new Statement.Mvoff(refs(), point("offset"), start.to(last()));
            case SCALE -> new Statement.Scale(ref(), number("scale factor"), start.to(last()));
            case GRP -> new Statement.Grp(refs(), start.to(last()));
            case UNGRP -> new Statement.Ungrp(ref(), start.to(last()));
            case COLOR -> colour(start);
            case LS -> new Statement.Ls(target(), start.to(last()));
            case AREA -> new Statement.Area(target(), start.to(last()));
            case PERIMETER -> new Statement.Perimeter(target(), start.to(last()));
            case SAVE -> new Statement.Save(path(), start.to(last()));
            case LOAD -> new Statement.Load(path(), start.to(last()));
            case EXPORT -> new Statement.Export(path(), start.to(last()));
            case UNDO -> new Statement.Undo(start);
            case REDO -> new Statement.Redo(start);
            case CLEAR -> new Statement.Clear(start);
            case HELP -> help(start);
        };
    }

    private Statement create(Span start) {
        Token kind = peek();
        if (!kind.is(Type.WORD)) {
            throw error("expected what to create: circle, rectangle or image", kind.span());
        }
        take();
        Expression<Shape> shape = switch (kind.text().toLowerCase(Locale.ROOT)) {
            case "circle" -> new Statement.NewCircle(radius(), point("centre"), kind.span().to(last()));
            case "rectangle", "rect" -> new Statement.NewRectangle(point("centre"), point("size"), kind.span().to(last()));
            case "image", "img" -> new Statement.NewImage(path(), point("centre"), kind.span().to(last()));
            default -> throw error("cannot create '" + kind.text() + "': the shapes are circle, rectangle and image",
                    kind.span());
        };
        return new Statement.New(shape, paints(), start.to(last()));
    }

    private Statement colour(Span start) {
        List<Ref> shapes = refs();
        List<Paint> paints;
        if (peek().is(Type.COLOR)) {
            Token colour = take();
            paints = List.of(new Paint(true, Rgb.parse(colour.text()), colour.span()));
        } else {
            paints = paints();
            if (paints.isEmpty()) {
                throw error("expected a colour like #2a9d8f, or fill and stroke", peek().span());
            }
        }
        return new Statement.Color(shapes, paints, start.to(last()));
    }

    private Statement help(Span start) {
        if (peek().is(Type.END)) {
            return new Statement.Help(null, start);
        }
        Token word = take();
        Verb about = Verb.of(word.text()).orElseThrow(() -> error("there is no command '" + word.text() + "'"
                + Verb.closest(word.text()).map(v -> ": did you mean " + v.keyword() + "?").orElse(""), word.span()));
        return new Statement.Help(about, start.to(word.span()));
    }

    private Num radius() {
        if (peek().is(Type.LPAREN)) {
            take();
            Num radius = number("radius");
            expect(Type.RPAREN, "')' after the radius");
            return radius;
        }
        return number("radius");
    }

    /** {@code (x,y)}; the comma may be left out. */
    private Pt point(String what) {
        Token open = peek();
        if (!open.is(Type.LPAREN)) {
            throw error("expected the " + what + " as (x,y), found " + open.describe(), open.span());
        }
        take();
        Num x = number("first coordinate of the " + what);
        if (peek().is(Type.COMMA)) {
            take();
        }
        Num y = number("second coordinate of the " + what);
        Token close = expect(Type.RPAREN, "')' to close the " + what);
        return new Pt(x, y, open.span().to(close.span()));
    }

    private Num number(String what) {
        Token token = peek();
        if (!token.is(Type.NUMBER)) {
            throw error("expected the " + what + ", a number, found " + token.describe(), token.span());
        }
        take();
        return new Num(Double.parseDouble(token.text()), token.span());
    }

    private Ref ref() {
        Token token = peek();
        if (!token.is(Type.WORD)) {
            throw error("expected the id of a shape, like obj0 or grp0, found " + token.describe(), token.span());
        }
        take();
        return new Ref(token.text(), token.span());
    }

    /** One or more ids, separated by spaces or commas. */
    private List<Ref> refs() {
        List<Ref> refs = new ArrayList<>();
        refs.add(ref());
        while (true) {
            if (peek().is(Type.COMMA)) {
                take();
                refs.add(ref());
            } else if (peek().is(Type.WORD) && !isPaint(peek())) {
                refs.add(ref());
            } else {
                return refs;
            }
        }
    }

    private List<Paint> paints() {
        List<Paint> paints = new ArrayList<>();
        while (isPaint(peek())) {
            Token which = take();
            Token value = peek();
            Rgb colour;
            if (value.is(Type.COLOR)) {
                colour = Rgb.parse(value.text());
            } else if (value.isWord("none")) {
                colour = null;
            } else {
                throw error("expected a colour like #2a9d8f, or none, after " + which.text(), value.span());
            }
            take();
            paints.add(new Paint(which.isWord("fill"), colour, which.span().to(value.span())));
        }
        return paints;
    }

    private static boolean isPaint(Token token) {
        return token.isWord("fill") || token.isWord("stroke");
    }

    private Target target() {
        Token token = peek();
        if (token.isWord("all")) {
            take();
            return new Target.All(token.span());
        }
        if (token.is(Type.WORD)) {
            String word = token.text().toLowerCase(Locale.ROOT);
            String kind = switch (word) {
                case "img", "images" -> "image";
                case "rect", "rectangles" -> "rectangle";
                case "circles" -> "circle";
                case "groups" -> "group";
                default -> word;
            };
            if (KINDS.contains(kind)) {
                take();
                return new Target.OfKind(kind, token.span());
            }
        }
        List<Ref> refs = refs();
        return new Target.Ids(refs, refs.getFirst().span().to(refs.getLast().span()));
    }

    private FilePath path() {
        Token token = peek();
        boolean parenthesised = token.is(Type.LPAREN);
        if (parenthesised) {
            take();
            token = peek();
        }
        if (!token.is(Type.STRING) && !token.is(Type.WORD)) {
            throw error("expected a file name, in quotes if it has spaces, found " + token.describe(), token.span());
        }
        take();
        if (token.text().isBlank()) {
            throw error("the file name is empty", token.span());
        }
        if (parenthesised) {
            expect(Type.RPAREN, "')' after the file name");
        }
        return new FilePath(token.text(), token.span());
    }

    private Token expect(Type type, String what) {
        Token token = peek();
        if (!token.is(type)) {
            throw error("expected " + what + ", found " + token.describe(), token.span());
        }
        return take();
    }

    private Token peek() {
        return tokens.get(next);
    }

    private Token take() {
        return tokens.get(next++);
    }

    private Span last() {
        return tokens.get(Math.max(0, next - 1)).span();
    }

    private ScriptError error(String message, Span span) {
        return new ScriptError(message, span);
    }
}
