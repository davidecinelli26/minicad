package minicad.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static minicad.model.ShapeTest.EPS;
import static minicad.model.ShapeTest.circle;
import static minicad.model.ShapeTest.rectangle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawingTest {

    private final Drawing drawing = new Drawing();

    private List<String> ids(List<Shape> shapes) {
        return shapes.stream().map(Shape::id).toList();
    }

    @Test
    void idsAreGivenInOrder() {
        assertEquals("obj0", drawing.add(circle(0, 0, 1)).id());
        assertEquals("obj1", drawing.add(rectangle(0, 0, 1, 1)).id());
        Group group = drawing.group(drawing.items());
        assertEquals("grp0", group.id());
        assertSame(group, drawing.find("grp0").orElseThrow());
    }

    @Test
    void movingAGroupKeepsItsLayout() {
        // The 2024 version moved every member onto the same point.
        Circle a = drawing.add(circle(0, 0, 1));
        Circle b = drawing.add(circle(10, 0, 1));
        Group group = drawing.group(List.of(a, b));
        group.moveTo(new Point(100, 100));
        assertEquals(new Point(95, 100), a.center());
        assertEquals(new Point(105, 100), b.center());
    }

    @Test
    void scalingAGroupScalesAboutItsCentre() {
        Circle a = drawing.add(circle(0, 0, 1));
        Circle b = drawing.add(circle(10, 0, 1));
        Group group = drawing.group(List.of(a, b));
        group.scale(2);
        assertEquals(new Point(-5, 0), a.center());
        assertEquals(new Point(15, 0), b.center());
        assertEquals(2, a.radius(), EPS);
    }

    @Test
    void groupsNest() {
        Circle a = drawing.add(circle(0, 0, 1));
        Circle b = drawing.add(circle(10, 0, 1));
        Rectangle c = drawing.add(rectangle(5, 20, 2, 2));
        Group inner = drawing.group(List.of(a, b));
        Group outer = drawing.group(List.of(inner, c));
        assertEquals(List.of("grp1"), ids(drawing.items()));
        assertEquals(2 * Math.PI + 4, outer.area(), EPS);
        assertEquals(outer, drawing.parentOf(inner).orElseThrow());
        assertEquals(inner, drawing.parentOf(a).orElseThrow());
        assertEquals(List.of("grp1", "grp0", "obj0", "obj1", "obj2"), drawing.shapes().map(Shape::id).toList());
    }

    @Test
    void aGroupTakesThePlaceOfItsTopmostMember() {
        Circle a = drawing.add(circle(0, 0, 1));
        Circle b = drawing.add(circle(0, 0, 1));
        drawing.add(circle(0, 0, 1));
        Circle d = drawing.add(circle(0, 0, 1));
        drawing.group(List.of(d, b));
        assertEquals(List.of("obj0", "obj2", "grp0"), ids(drawing.items()));
        assertEquals(List.of("obj1", "obj3"), ids(((Group) drawing.items().getLast()).members()));
        drawing.ungroup((Group) drawing.items().getLast());
        assertEquals(List.of("obj0", "obj2", "obj1", "obj3"), ids(drawing.items()));
        assertFalse(drawing.find("grp0").isPresent());
        assertSame(a, drawing.items().getFirst());
    }

    @Test
    void onlySiblingsCanBeGrouped() {
        Circle a = drawing.add(circle(0, 0, 1));
        Circle b = drawing.add(circle(0, 0, 1));
        Circle c = drawing.add(circle(0, 0, 1));
        drawing.group(List.of(a, b));
        assertThrows(IllegalArgumentException.class, () -> drawing.group(List.of(a, c)));
        assertThrows(IllegalArgumentException.class, () -> drawing.group(List.of(c, c)));
    }

    @Test
    void removingTheLastMemberRemovesTheGroup() {
        Circle a = drawing.add(circle(0, 0, 1));
        drawing.group(List.of(a));
        drawing.remove(a);
        assertTrue(drawing.isEmpty());
        assertFalse(drawing.find("grp0").isPresent());
    }

    @Test
    void addingACopyGivesNewIds() {
        Circle a = drawing.add(circle(0, 0, 1));
        Circle b = drawing.add(circle(5, 0, 1));
        Group group = drawing.group(List.of(a, b));
        Group copy = drawing.add(group.copy());
        assertEquals("grp1", copy.id());
        assertEquals(List.of("obj2", "obj3"), ids(copy.members()));
    }

    @Test
    void mementoRestoresShapesIdsAndCounters() {
        Circle a = drawing.add(circle(0, 0, 1));
        Drawing.Memento before = drawing.save();
        a.moveBy(50, 0);
        drawing.add(circle(0, 0, 2));
        drawing.restore(before);
        assertEquals(1, drawing.items().size());
        assertEquals(new Point(0, 0), drawing.find("obj0").orElseThrow().center());
        assertEquals("obj1", drawing.add(circle(0, 0, 1)).id(), "the counter came back too");
        drawing.restore(before);
        assertEquals(1, drawing.items().size(), "a memento can be restored more than once");
    }

    @Test
    void listenersHearShapesAndStructure() {
        List<DrawingListener.Change> heard = new ArrayList<>();
        drawing.addListener(heard::add);
        Circle a = drawing.add(circle(0, 0, 1));
        Circle b = drawing.add(circle(0, 0, 1));
        Group group = drawing.group(List.of(a, b));
        heard.clear();
        a.moveBy(1, 0);
        assertEquals(List.of(DrawingListener.Change.SHAPE), heard, "a member inside a group is observed too");
        heard.clear();
        drawing.ungroup(group);
        group.moveBy(1, 1);
        assertEquals(List.of(DrawingListener.Change.STRUCTURE, DrawingListener.Change.SHAPE,
                DrawingListener.Change.SHAPE), heard, "the old group is no longer observed, its members are");
    }

    @Test
    void clearStartsTheIdsAgain() {
        drawing.add(circle(0, 0, 1));
        drawing.clear();
        assertTrue(drawing.isEmpty());
        assertEquals("obj0", drawing.add(circle(0, 0, 1)).id());
    }
}
