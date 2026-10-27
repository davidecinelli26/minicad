package minicad.ui;

import minicad.app.Diagnostic;
import minicad.app.Editor;
import minicad.app.Output;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectTreeTest {

    private final Editor editor = new Editor(new Output() {
        @Override
        public void print(String text) {
        }

        @Override
        public void error(Diagnostic diagnostic) {
            throw new AssertionError(diagnostic.render());
        }
    });

    @Test
    void rebuildingTheTreeKeepsTheSelection() {
        // The tree rebuilds itself when shapes are added; that once cleared the shared selection.
        Selection selection = new Selection();
        new ObjectTree(editor.drawing(), selection);
        editor.run("new circle (5) (0,0)");
        editor.run("new circle (5) (20,0)");
        editor.run("grp obj0 obj1");
        selection.set("grp0");
        editor.run("copy grp0");
        editor.run("undo");
        assertEquals(Set.of("grp0"), selection.ids());
    }
}
