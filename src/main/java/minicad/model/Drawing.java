package minicad.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The document: the shapes in paint order, bottom first. It is the Subject the views observe
 * (Observer) and the Originator of the snapshots that undo is made of (Memento). It is also the one
 * place where ids are given; in the 2024 version they came from static counters in two singletons.
 */
public final class Drawing {

    private final List<Shape> items = new ArrayList<>();
    private final Map<String, Shape> byId = new HashMap<>();
    private final List<DrawingListener> listeners = new ArrayList<>();
    private final ShapeListener relay = shape -> fire(DrawingListener.Change.SHAPE);
    private int nextFigure;
    private int nextGroup;

    /** The shapes at the top level, bottom first. */
    public List<Shape> items() {
        return Collections.unmodifiableList(items);
    }

    /** Every shape, groups and their members included, depth first. */
    public Stream<Shape> shapes() {
        return items.stream().flatMap(Drawing::tree);
    }

    private static Stream<Shape> tree(Shape shape) {
        return shape instanceof Group group
                ? Stream.concat(Stream.of(group), group.members().stream().flatMap(Drawing::tree))
                : Stream.of(shape);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Optional<Shape> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<Bounds> bounds() {
        return items.stream().map(Shape::bounds).reduce(Bounds::union);
    }

    /** The group the shape is a member of, or empty if the shape is at the top level. */
    public Optional<Group> parentOf(Shape shape) {
        requireMember(shape);
        return shapes()
                .filter(s -> s instanceof Group group && group.memberList().contains(shape))
                .map(Group.class::cast)
                .findFirst();
    }

    /** Adds a shape on top of the others, giving it, and its members, new ids. */
    public <S extends Shape> S add(S shape) {
        if (contains(shape)) {
            throw new IllegalArgumentException(shape.id() + " is already in the drawing");
        }
        number(shape);
        items.add(shape);
        index(shape);
        fire(DrawingListener.Change.STRUCTURE);
        return shape;
    }

    /** Removes a shape; a group left with no members goes too. */
    public void remove(Shape shape) {
        detach(shape);
        fire(DrawingListener.Change.STRUCTURE);
    }

    private void detach(Shape shape) {
        Optional<Group> parent = parentOf(shape);
        siblings(parent).remove(shape);
        unindex(shape);
        if (parent.isPresent() && parent.get().memberList().isEmpty()) {
            detach(parent.get());
        }
    }

    /**
     * Puts shapes that share the same parent into a new group, which takes the place of the topmost
     * of them. Groups can be grouped too, at any depth.
     */
    public Group group(List<? extends Shape> shapes) {
        if (shapes.isEmpty()) {
            throw new IllegalArgumentException("nothing to group");
        }
        Set<Shape> chosen = Collections.newSetFromMap(new IdentityHashMap<>());
        Shape first = shapes.getFirst();
        Optional<Group> parent = parentOf(first);
        for (Shape shape : shapes) {
            if (!chosen.add(shape)) {
                throw new IllegalArgumentException(shape.id() + " is listed twice");
            }
            if (!parentOf(shape).equals(parent)) {
                throw new IllegalArgumentException(first.id() + " and " + shape.id()
                        + " are not in the same group: shapes are grouped with their siblings");
            }
        }
        List<Shape> siblings = siblings(parent);
        List<Shape> members = siblings.stream().filter(chosen::contains).toList();
        int at = siblings.indexOf(members.getLast()) - (members.size() - 1);
        siblings.removeIf(chosen::contains);
        Group group = new Group(members);
        group.setId("grp" + nextGroup++);
        siblings.add(at, group);
        byId.put(group.id(), group);
        group.addListener(relay);
        fire(DrawingListener.Change.STRUCTURE);
        return group;
    }

    /** Dissolves a group: its members take its place, and keep their ids. */
    public List<Shape> ungroup(Group group) {
        List<Shape> siblings = siblings(parentOf(group));
        int at = siblings.indexOf(group);
        List<Shape> members = List.copyOf(group.memberList());
        siblings.remove(at);
        siblings.addAll(at, members);
        byId.remove(group.id());
        group.removeListener(relay);
        fire(DrawingListener.Change.STRUCTURE);
        return members;
    }

    /** Removes everything and starts the ids again from obj0 and grp0. */
    public void clear() {
        items.forEach(this::unindex);
        items.clear();
        nextFigure = 0;
        nextGroup = 0;
        fire(DrawingListener.Change.STRUCTURE);
    }

    /** A snapshot of the whole drawing, ids and counters included. */
    public Memento save() {
        return new Memento(items.stream().map(Shape::copy).toList(), nextFigure, nextGroup);
    }

    /** Puts the drawing back as it was when the memento was saved. */
    public void restore(Memento memento) {
        items.forEach(this::unindex);
        items.clear();
        for (Shape shape : memento.items) {
            Shape copy = shape.copy();
            items.add(copy);
            index(copy);
        }
        nextFigure = memento.nextFigure;
        nextGroup = memento.nextGroup;
        fire(DrawingListener.Change.STRUCTURE);
    }

    /** The Memento: an immutable snapshot only the drawing can read. */
    public static final class Memento {

        private final List<Shape> items;
        private final int nextFigure;
        private final int nextGroup;

        private Memento(List<Shape> items, int nextFigure, int nextGroup) {
            this.items = items;
            this.nextFigure = nextFigure;
            this.nextGroup = nextGroup;
        }
    }

    public void addListener(DrawingListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DrawingListener listener) {
        listeners.remove(listener);
    }

    private void fire(DrawingListener.Change change) {
        for (DrawingListener listener : List.copyOf(listeners)) {
            listener.drawingChanged(change);
        }
    }

    private boolean contains(Shape shape) {
        return shape.id() != null && byId.get(shape.id()) == shape;
    }

    private void requireMember(Shape shape) {
        if (!contains(shape)) {
            throw new IllegalArgumentException(shape + " is not in the drawing");
        }
    }

    private List<Shape> siblings(Optional<Group> parent) {
        return parent.map(Group::memberList).orElse(items);
    }

    /** Ids in the order a script would create them: members before their group. */
    private void number(Shape shape) {
        if (shape instanceof Group group) {
            group.members().forEach(this::number);
            group.setId("grp" + nextGroup++);
        } else {
            shape.setId("obj" + nextFigure++);
        }
    }

    private void index(Shape shape) {
        byId.put(shape.id(), shape);
        shape.addListener(relay);
        if (shape instanceof Group group) {
            group.members().forEach(this::index);
        }
    }

    private void unindex(Shape shape) {
        byId.remove(shape.id());
        shape.removeListener(relay);
        if (shape instanceof Group group) {
            group.members().forEach(this::unindex);
        }
    }
}
