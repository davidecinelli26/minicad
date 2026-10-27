package minicad.app;

import minicad.io.ScriptWriter;
import minicad.model.Circle;
import minicad.model.Group;
import minicad.model.Picture;
import minicad.model.Point;
import minicad.model.Shape;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorTest {

    /** Collects what the editor prints. */
    static final class Recorder implements Output {
        final List<String> lines = new ArrayList<>();
        final List<Diagnostic> errors = new ArrayList<>();

        @Override
        public void print(String text) {
            lines.add(text);
        }

        @Override
        public void error(Diagnostic diagnostic) {
            errors.add(diagnostic);
        }
    }

    @TempDir
    Path folder;

    private Recorder out;
    private Editor editor;

    @BeforeEach
    void setUp() {
        out = new Recorder();
        editor = new Editor(out, folder);
    }

    private void run(String... lines) {
        for (String line : lines) {
            assertTrue(editor.run(line), () -> line + " failed: " + out.errors);
        }
    }

    private Shape shape(String id) {
        return editor.drawing().find(id).orElseThrow();
    }

    /** The drawing as text: two drawings are equal if they save the same. */
    private String saved() {
        return ScriptWriter.write(editor.drawing());
    }

    private Path image(String name, int width, int height) throws IOException {
        Path file = folder.resolve(name);
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", file.toFile());
        return file;
    }

    @Test
    void theScenarioOfTheReport() {
        run("new circle (80) (200,200)", "new rectangle (100,100) (50,30)", "new circle (20) (400,300)");
        assertEquals(List.of("obj0", "obj1", "obj2"), out.lines);
        run("grp obj0 obj1", "mvoff grp0 (10,0)", "scale grp0 2");
        // The group spans (85,85)-(290,280): scaling doubles each member's distance from (187.5,182.5).
        assertEquals(new Point(232.5, 217.5), shape("obj0").center());
        assertEquals(new Point(187.5, 182.5), shape("grp0").center());
        out.lines.clear();
        run("ls all");
        assertEquals(List.of(
                "grp0  group  at (187.5,182.5)  2 members",
                "  obj0  circle  at (232.5,217.5)  radius 160  stroke #334155",
                "  obj1  rectangle  at (32.5,17.5)  size 100x60  stroke #334155",
                "obj2  circle  at (400,300)  radius 20  stroke #334155"), out.lines);
    }

    @Test
    void groupsComeInPaintOrder() {
        run("new circle (1) (0,0)", "new circle (1) (5,0)", "grp obj1 obj0");
        assertEquals(List.of("obj0", "obj1"),
                ((Group) shape("grp0")).members().stream().map(Shape::id).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "new circle (5) (1,1) fill #e9c46a",
            "copy grp0 (50,0)",
            "del obj0",
            "del grp0",
            "mv grp0 (300,300)",
            "mvoff obj0 obj2 (1,1)",
            "scale grp0 0.5",
            "grp grp0 obj2",
            "ungrp grp0",
            "color grp0 fill #264653 stroke none",
            "clear"
    })
    void everyEditCanBeUndoneAndRedone(String edit) {
        run("new circle (10) (0,0)", "new rectangle (40,0) (20,10)", "grp obj0 obj1", "new circle (3) (90,90)");
        String before = saved();
        run(edit);
        String after = saved();
        assertNotEquals(before, after, "the edit changed something");
        run("undo");
        assertEquals(before, saved());
        run("redo");
        assertEquals(after, saved());
    }

    @Test
    void queriesDoNotTouchTheHistory() {
        run("new circle (1) (0,0)", "ls all", "area all", "perimeter obj0", "help", "undo");
        assertTrue(editor.drawing().isEmpty(), "undo undid the circle, not the queries");
    }

    @Test
    void measuresPrintEachShapeAndTheTotal() {
        run("new rectangle (0,0) (10,10)", "new rectangle (0,0) (2,5)");
        out.lines.clear();
        run("area all");
        assertEquals(List.of("obj0  100", "obj1  10", "total  110"), out.lines);
        out.lines.clear();
        run("perimeter obj1");
        assertEquals(List.of("obj1  14"), out.lines);
        out.lines.clear();
        run("ls circles");
        assertEquals(List.of("there are no circles"), out.lines);
    }

    @Test
    void copyDuplicatesAWholeGroup() {
        run("new circle (1) (0,0)", "new circle (1) (10,0)", "grp obj0 obj1", "copy grp0 (0,50)");
        Group copy = assertInstanceOf(Group.class, shape("grp1"));
        assertEquals(new Point(5, 50), copy.center());
        assertEquals(List.of("obj2", "obj3"), copy.members().stream().map(Shape::id).toList());
        assertEquals(new Point(5, 0), shape("grp0").center(), "the original did not move");
    }

    @Test
    void mistakesAreReportedAndChangeNothing() {
        run("new circle (10) (0,0)");
        String before = saved();
        for (String line : List.of("mv obj9 (1,2)", "scale obj0 -2", "ungrp obj0", "new circle (0) (1,1)",
                "grp obj0 obj0", "mv obj0 (1,", "delete obj0", "load \"missing.mcad\"")) {
            assertFalse(editor.run(line), line);
        }
        assertEquals(before, saved());
        assertEquals(8, out.errors.size());
        Diagnostic unknown = out.errors.getFirst();
        assertEquals("""
                mv obj9 (1,2)
                   ^^^^ there is no shape obj9""", unknown.render());
        assertTrue(out.errors.get(6).message().contains("did you mean del?"));
    }

    @Test
    void saveThenLoadGivesTheSameDrawing() throws IOException {
        image("tile.png", 8, 4);
        run("new circle (10) (0,0) fill #e76f51", "new rectangle (30,0) (20,10) stroke none fill #2a9d8f",
                "new image tile.png (60,60)", "scale obj2 1.5", "grp obj0 obj1", "grp grp0 obj2",
                "new circle (2) (1,1) stroke #000");
        String before = saved();
        run("save drawing.mcad", "clear", "load drawing.mcad");
        assertEquals(before, saved());
        assertEquals(1, editor.images().size(), "the image file was decoded once");
        Picture picture = assertInstanceOf(Picture.class, shape("obj2"));
        assertEquals(12 * 6, picture.area(), 1e-9);
    }

    @Test
    void loadIsAtomicAndUndoable() throws IOException {
        run("new circle (1) (0,0)");
        Files.writeString(folder.resolve("good.mcad"), "new circle (5) (1,1)\nnew circle (6) (2,2)\n");
        Files.writeString(folder.resolve("bad.mcad"), "new circle (5) (1,1)\nnew circle (6) (2,2\n");
        String before = saved();
        assertFalse(editor.run("load bad.mcad"));
        assertEquals(before, saved(), "nothing of the broken file was kept");
        Diagnostic error = out.errors.getLast();
        assertTrue(error.message().startsWith("bad.mcad, line 2: "), error.message());
        assertEquals("new circle (6) (2,2", error.line());
        run("load good.mcad");
        assertEquals(2, editor.drawing().items().size());
        run("undo");
        assertEquals(before, saved());
    }

    @Test
    void imagesAreShared() throws IOException {
        image("tree.png", 16, 32);
        run("new image \"tree.png\" (0,0)", "copy obj0", "new image (tree.png) (50,50)");
        assertEquals(1, editor.images().size());
        assertSame(((Picture) shape("obj0")).image(), ((Picture) shape("obj2")).image());
        assertFalse(editor.run("new image missing.png (0,0)"));
        assertTrue(out.errors.getLast().message().startsWith("no such file"));
    }

    @Test
    void exportWritesAnSvg() throws IOException {
        image("dot.png", 2, 2);
        run("new circle (10) (0,0) fill #e9c46a", "new rectangle (40,0) (20,10)", "grp obj0 obj1",
                "new image dot.png (0,40)", "export out/drawing.svg");
        String svg = Files.readString(folder.resolve("out/drawing.svg"));
        assertTrue(svg.startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"-30 -30 100 91\""), svg);
        assertTrue(svg.contains("<circle id=\"obj0\" cx=\"0\" cy=\"0\" r=\"10\" fill=\"#e9c46a\" stroke=\"#334155\""));
        assertTrue(svg.contains("<g id=\"grp0\">"));
        assertTrue(svg.contains("href=\"data:image/png;base64,"));
        assertFalse(editor.run("export drawing.png"), "the extension is checked");
    }

    @Test
    void headlessRunsAFileAndExportsIt() throws IOException {
        Path script = folder.resolve("garden.mcad");
        Path svg = folder.resolve("garden.svg");
        Files.writeString(script, "new circle (5) (0,0)\nls all\n");
        ByteArrayOutputStream stdout = new ByteArrayOutputStream(), stderr = new ByteArrayOutputStream();
        int code = Headless.run(new String[]{script.toString(), "--svg", svg.toString()},
                new PrintStream(stdout, true), new PrintStream(stderr, true));
        assertEquals(0, code, stderr::toString);
        assertTrue(stdout.toString().contains("obj0  circle  at (0,0)  radius 5"));
        assertTrue(Files.exists(svg));
        assertEquals(1, Headless.run(new String[]{folder.resolve("none.mcad").toString()},
                new PrintStream(stdout), new PrintStream(stderr)));
        assertEquals(2, Headless.run(new String[]{"a.mcad", "b.mcad"}, new PrintStream(stdout), new PrintStream(stderr)));
    }

    @Test
    void codeCanRunCommandsToo() {
        assertTrue(editor.run(ws -> ws.drawing().add(new Circle(new Point(0, 0), 1, minicad.model.Style.OUTLINE)),
                "example"));
        assertEquals("example", editor.history().nextUndo().orElseThrow());
    }
}
