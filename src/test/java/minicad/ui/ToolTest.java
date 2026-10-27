package minicad.ui;

import minicad.app.Diagnostic;
import minicad.app.Editor;
import minicad.app.Output;
import minicad.model.Drawing;
import minicad.model.Point;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The tools write command lines: these tests read them, with no window at all. */
class ToolTest {

    private final Editor editor = new Editor(new Output() {
        @Override
        public void print(String text) {
        }

        @Override
        public void error(Diagnostic diagnostic) {
            throw new AssertionError(diagnostic.render());
        }
    });
    private final Selection selection = new Selection();
    private final List<String> commands = new ArrayList<>();
    private boolean snapping = true;

    private final Tool.Context context = new Tool.Context() {
        @Override
        public Drawing drawing() {
            return editor.drawing();
        }

        @Override
        public Selection selection() {
            return selection;
        }

        @Override
        public boolean run(String command) {
            commands.add(command);
            return editor.run(command);
        }

        @Override
        public Point snap(Point p) {
            return snapping ? new Point(Math.round(p.x() / 10) * 10, Math.round(p.y() / 10) * 10) : p;
        }

        @Override
        public void panBy(double dx, double dy) {
            commands.add("pan " + dx + " " + dy);
        }

        @Override
        public void repaint() {
        }
    };

    private static Tool.Pointer at(double x, double y) {
        return new Tool.Pointer(new Point(x, y), x, y, false);
    }

    private static void drag(Tool tool, Tool.Pointer from, Tool.Pointer to) {
        tool.pressed(from);
        tool.dragged(to);
        tool.released(to);
    }

    @Test
    void dragACircleFromItsCentre() {
        drag(new ShapeTool.CircleTool(context), at(101, 99), at(131, 139));
        assertEquals(List.of("new circle (50) (100,100)"), commands);
        assertEquals(Set.of("obj0"), selection.ids(), "the new shape is selected");
    }

    @Test
    void dragARectangleCornerToCorner() {
        snapping = false;
        drag(new ShapeTool.RectangleTool(context), at(10, 50), at(70, 20));
        assertEquals(List.of("new rectangle (40,35) (60,30)"), commands);
    }

    @Test
    void shiftMakesASquare() {
        snapping = false;
        Tool tool = new ShapeTool.RectangleTool(context);
        tool.pressed(at(0, 0));
        tool.released(new Tool.Pointer(new Point(30, -10), 30, -10, true));
        assertEquals(List.of("new rectangle (15,-15) (30,30)"), commands);
    }

    @Test
    void aClickIsNotAShape() {
        drag(new ShapeTool.CircleTool(context), at(10, 10), at(10, 10));
        assertTrue(commands.isEmpty());
    }

    @Test
    void draggingTheSelectionIsOneMove() {
        editor.run("new circle (10) (0,0)");
        editor.run("new circle (10) (100,0)");
        editor.run("grp obj0 obj1");
        drag(new SelectTool(context), at(2, 3), at(32, 43));
        assertEquals(List.of("mvoff grp0 (30,40)"), commands, "clicking a member grabs its whole group");
        assertEquals(new Point(130, 40), editor.drawing().find("obj1").orElseThrow().center());
        editor.run("undo");
        assertEquals(new Point(100, 0), editor.drawing().find("obj1").orElseThrow().center());
    }

    @Test
    void shiftClickAddsToTheSelectionAndAClickOnNothingClearsIt() {
        editor.run("new circle (10) (0,0)");
        editor.run("new circle (10) (100,0)");
        SelectTool tool = new SelectTool(context);
        drag(tool, at(0, 0), at(0, 0));
        tool.pressed(new Tool.Pointer(new Point(100, 0), 100, 0, true));
        assertEquals(Set.of("obj0", "obj1"), selection.ids());
        drag(tool, at(500, 500), at(500, 500));
        assertTrue(selection.isEmpty());
        assertTrue(commands.isEmpty(), "selecting is not an edit");
    }

    @Test
    void panningMovesTheViewNotTheDrawing() {
        drag(new PanTool(context), at(10, 10), at(25, 5));
        assertEquals(List.of("pan 15.0 -5.0"), commands);
    }

    @Test
    void completion() {
        List<String> ids = List.of("obj0", "obj1", "obj10", "grp0");
        assertEquals("perimeter ", Completer.complete("per", ids).line());
        assertEquals(List.of("mv", "mvoff"), Completer.complete("m", ids).choices());
        assertEquals("ls groups ", Completer.complete("ls grou", ids).line());
        assertEquals(List.of("groups", "grp0"), Completer.complete("ls gr", ids).choices());
        assertEquals("mv obj", Completer.complete("mv o", ids).line());
        assertEquals(List.of("obj0", "obj1", "obj10"), Completer.complete("mv o", ids).choices());
        assertEquals("ungrp grp0 ", Completer.complete("ungrp ", ids).line());
        assertEquals("new circle ", Completer.complete("new c", ids).line());
        assertEquals("grp obj0,obj10 ", Completer.complete("grp obj0,obj10", ids).line());
    }
}
