package minicad.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {

    private static Statement parse(String line) {
        return Parser.parse(line).orElseThrow();
    }

    private static ScriptError error(String line) {
        return assertThrows(ScriptError.class, () -> Parser.parse(line));
    }

    /** The text the error underlines. */
    private static String underlined(String line) {
        Span span = error(line).span();
        return line.substring(span.start(), span.end());
    }

    @Test
    void tokensKnowTheirColumns() {
        List<Token> tokens = Lexer.tokenize("mv obj1 ( 10 , -2.5 )");
        assertEquals(List.of("mv", "obj1", "(", "10", ",", "-2.5", ")", ""),
                tokens.stream().map(Token::text).toList());
        assertEquals(new Span(3, 7), tokens.get(1).span());
        assertEquals(Token.Type.END, tokens.getLast().type());
    }

    @Test
    void stringsAndCommentsAndColours() {
        List<Token> tokens = Lexer.tokenize("new image \"my \\\"tree\\\".png\" (1,2) // a comment");
        assertEquals("my \"tree\".png", tokens.get(2).text());
        assertEquals(Token.Type.END, tokens.get(8).type());
        assertEquals(Token.Type.COLOR, Lexer.tokenize("#2a9d8f").getFirst().type());
        assertTrue(Parser.parse("   // only a comment").isEmpty());
        assertTrue(Parser.parse("").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "new circle (80) (200,200)",
            "new circle 80 (200 200)",
            "new rectangle (10,10) (30,20) fill #a7c957 stroke none",
            "new image (\"tree.png\") (5,5)",
            "new img tree.png (5,5)",
            "copy obj0",
            "copy grp0 (10,0)",
            "del obj0 obj1, grp2",
            "mv obj0 (1,2)",
            "mvoff obj0 obj1 (-5,5)",
            "scale grp0 1.5",
            "grp obj0, obj1 obj2",
            "ungrp grp0",
            "color obj0 obj1 #f00",
            "color grp0 fill none stroke #264653",
            "ls all", "ls groups", "ls circle", "ls obj1 obj2",
            "area all", "area images", "perimeter grp0",
            "save garden.mcad", "load \"my garden.mcad\"", "export out/garden.svg",
            "undo", "redo", "clear", "help", "help mvoff",
            "NEW CIRCLE (1) (0,0)"
    })
    void everyCommandParses(String line) {
        parse(line);
    }

    @Test
    void theTreeHasTheValues() {
        Statement.Mvoff move = assertInstanceOf(Statement.Mvoff.class, parse("mvoff obj0, grp1 (3,-4)"));
        assertEquals(List.of("obj0", "grp1"), move.shapes().stream().map(Terminal.Ref::id).toList());
        assertEquals(-4, move.offset().y().value());
        Statement.New create = assertInstanceOf(Statement.New.class, parse("new circle (2) (1,1) fill #fff"));
        assertEquals(1, create.paints().size());
        assertInstanceOf(Target.OfKind.class, ((Statement.Ls) parse("ls rect")).target());
    }

    @Test
    void errorsPointAtTheMistake() {
        assertEquals("mvv", underlined("mvv obj0 (1,2)"));
        assertTrue(error("mvv obj0 (1,2)").getMessage().endsWith("did you mean mv?"));
        assertEquals("", underlined("mv obj0 (1,2"), "the end of the line, where ')' is missing");
        assertEquals("abc", underlined("scale obj0 abc"));
        assertEquals("\"tree.png (1,2)", underlined("new image \"tree.png (1,2)"));
        assertEquals("#12", underlined("color obj0 #12"));
        assertEquals("@", underlined("ls @"));
        assertEquals("extra", underlined("undo extra"));
        assertEquals("obj1", underlined("mv obj0 obj1 (1,2)"));
        assertEquals("triangle", underlined("new triangle (1,2)"));
    }

    @Test
    void errorsAfterTheCommandNameCarryItsUsage() {
        assertEquals("mv ID (x,y)", error("mv obj0").hint());
        assertNull(error("frobnicate").hint());
    }
}
