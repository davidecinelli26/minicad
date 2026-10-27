package minicad.ui;

import minicad.model.Drawing;
import minicad.model.Group;
import minicad.model.Shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The ids of the selected shapes, shared by the canvas and the object tree, which observe it. Ids,
 * not shapes: after undo the drawing holds new copies, but the same ids stay selected.
 */
final class Selection {

    private final Set<String> ids = new LinkedHashSet<>();
    private final List<Runnable> listeners = new ArrayList<>();

    Set<String> ids() {
        return Set.copyOf(ids);
    }

    boolean isEmpty() {
        return ids.isEmpty();
    }

    boolean contains(String id) {
        return ids.contains(id);
    }

    void set(Collection<String> selected) {
        if (!ids.equals(new LinkedHashSet<>(selected))) {
            ids.clear();
            ids.addAll(selected);
            fire();
        }
    }

    void set(String id) {
        set(List.of(id));
    }

    void toggle(String id) {
        if (!ids.remove(id)) {
            ids.add(id);
        }
        fire();
    }

    void clear() {
        set(List.of());
    }

    /** Forgets ids that are no longer in the drawing. */
    void prune(Drawing drawing) {
        if (ids.removeIf(id -> drawing.find(id).isEmpty())) {
            fire();
        }
    }

    /**
     * The selected shapes, without those inside another selected group: moving a group and one of
     * its members would move the member twice.
     */
    List<Shape> shapes(Drawing drawing) {
        List<Shape> shapes = ids.stream().map(drawing::find).flatMap(Optional::stream).toList();
        return shapes.stream().filter(shape -> !hasSelectedAncestor(shape, drawing)).toList();
    }

    private boolean hasSelectedAncestor(Shape shape, Drawing drawing) {
        Optional<Group> parent = drawing.parentOf(shape);
        while (parent.isPresent()) {
            if (ids.contains(parent.get().id())) {
                return true;
            }
            parent = drawing.parentOf(parent.get());
        }
        return false;
    }

    void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void fire() {
        listeners.forEach(Runnable::run);
    }
}
