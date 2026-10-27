package minicad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Anything that can be in a drawing. Four patterns meet here:
 * <ul>
 *   <li><b>Composite</b>: this is the Component; a {@link Group} is a Shape too, so every command
 *       handles one figure and a whole group the same way.</li>
 *   <li><b>Prototype</b>: {@link #copy()} returns an independent deep copy, used by the
 *       {@code copy} command and by the snapshots that make undo work.</li>
 *   <li><b>Observer</b>: shapes are Subjects and tell their {@link ShapeListener}s when they change.</li>
 *   <li><b>Visitor</b>: {@link #accept} lets drawing, exporting and saving be written outside the
 *       model, which so does not depend on Swing or on file formats.</li>
 * </ul>
 * Positions are centres, and the id is given by the {@link Drawing} that holds the shape.
 */
public abstract class Shape {

    private String id;
    private final List<ShapeListener> listeners = new ArrayList<>(1);

    /** {@code obj<n>} or {@code grp<n>}, or null if the shape is not in a drawing yet. */
    public String id() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    /** {@code circle}, {@code rectangle}, {@code image} or {@code group}. */
    public abstract String kind();

    public abstract Bounds bounds();

    public Point center() {
        return bounds().center();
    }

    public abstract void moveBy(double dx, double dy);

    public void moveTo(Point target) {
        Point c = center();
        moveBy(target.x() - c.x(), target.y() - c.y());
    }

    /** Scales the shape by {@code factor} about {@code origin}: its size, and its distance from it. */
    public abstract void scale(double factor, Point origin);

    /** Scales the shape about its own centre. */
    public void scale(double factor) {
        scale(factor, center());
    }

    public abstract boolean contains(Point p);

    public abstract double area();

    public abstract double perimeter();

    /** Changes the colours, of every figure inside it in the case of a group. */
    public abstract void restyle(UnaryOperator<Style> change);

    /** A deep copy that keeps the id; the drawing gives new ids to copies it adds. */
    public abstract Shape copy();

    public abstract <R> R accept(ShapeVisitor<R> visitor);

    public void addListener(ShapeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ShapeListener listener) {
        listeners.remove(listener);
    }

    protected void changed() {
        for (ShapeListener listener : List.copyOf(listeners)) {
            listener.shapeChanged(this);
        }
    }

    protected <S extends Shape> S withMyId(S copy) {
        copy.setId(id);
        return copy;
    }

    static double positive(double value, String what) {
        if (!(value > 0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(what + " must be a positive number");
        }
        return value;
    }

    @Override
    public String toString() {
        return (id == null ? "new" : id) + " " + kind();
    }
}
