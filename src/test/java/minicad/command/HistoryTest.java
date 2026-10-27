package minicad.command;

import minicad.model.Circle;
import minicad.model.Drawing;
import minicad.model.ImageStore;
import minicad.model.Point;
import minicad.model.Style;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryTest {

    /** A workspace with nothing but a drawing and a history. */
    private final class Bench implements Workspace {
        final Drawing drawing = new Drawing();
        final History history;
        final List<String> printed = new ArrayList<>();

        Bench(int limit) {
            history = new History(limit);
        }

        @Override public Drawing drawing() { return drawing; }
        @Override public History history() { return history; }
        @Override public ImageStore images() { return new ImageStore(); }
        @Override public Path resolve(String path) { return Path.of(path); }
        @Override public void print(String text) { printed.add(text); }
        @Override public void open(Path script) { throw new UnsupportedOperationException(); }
    }

    private final Bench bench = new Bench(History.DEFAULT_LIMIT);

    private void add(double x) {
        bench.history.run(new Create(new Circle(new Point(x, 0), 1, Style.OUTLINE)), "new " + x, bench);
    }

    @Test
    void undoAndRedo() {
        add(1);
        add(2);
        assertEquals(Optional.of("new 2.0"), bench.history.undo(bench.drawing));
        assertEquals(1, bench.drawing.items().size());
        assertEquals(Optional.of("new 2.0"), bench.history.redo(bench.drawing));
        assertEquals(2, bench.drawing.items().size());
        assertEquals(Optional.empty(), bench.history.redo(bench.drawing));
    }

    @Test
    void aNewEditForgetsWhatCouldBeRedone() {
        add(1);
        bench.history.undo(bench.drawing);
        add(2);
        assertEquals(Optional.empty(), bench.history.nextRedo());
    }

    @Test
    void queriesAreNotRecorded() {
        // In the 2024 version, undo after ls undid the ls.
        add(1);
        bench.history.run((Query) ws -> ws.print("listing"), "ls all", bench);
        assertEquals(Optional.of("new 1.0"), bench.history.nextUndo());
        assertEquals(List.of("obj0", "listing"), bench.printed);
    }

    @Test
    void aFailingCommandLeavesNoTrace() {
        add(1);
        Command halfway = ws -> {
            ws.drawing().items().getFirst().moveBy(100, 0);
            throw new CommandException("failed halfway");
        };
        assertThrows(CommandException.class, () -> bench.history.run(halfway, "broken", bench));
        assertEquals(new Point(1, 0), bench.drawing.find("obj0").orElseThrow().center());
        assertEquals(Optional.of("new 1.0"), bench.history.nextUndo());
    }

    @Test
    void theHistoryIsBounded() {
        Bench small = new Bench(2);
        for (int i = 0; i < 4; i++) {
            small.history.run(new Create(new Circle(new Point(i, 0), 1, Style.OUTLINE)), "new", small);
        }
        assertTrue(small.history.undo(small.drawing).isPresent());
        assertTrue(small.history.undo(small.drawing).isPresent());
        assertTrue(small.history.undo(small.drawing).isEmpty());
        assertEquals(2, small.drawing.items().size());
    }
}
