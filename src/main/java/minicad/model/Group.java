package minicad.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The Composite: shapes, groups included, handled as one. Moving a group translates every member,
 * and scaling it scales the members about the centre of the group, so that it keeps its layout.
 * Area and perimeter are the sums over the members.
 */
public final class Group extends Shape {

    private final List<Shape> members;

    public Group(List<? extends Shape> members) {
        if (members.isEmpty()) {
            throw new IllegalArgumentException("a group needs at least one shape");
        }
        this.members = new ArrayList<>(members);
    }

    /** The members, bottom first. */
    public List<Shape> members() {
        return Collections.unmodifiableList(members);
    }

    /** The live list, for the drawing to edit. */
    List<Shape> memberList() {
        return members;
    }

    @Override
    public String kind() {
        return "group";
    }

    @Override
    public Bounds bounds() {
        return members.stream().map(Shape::bounds).reduce(Bounds::union).orElseThrow();
    }

    @Override
    public void moveBy(double dx, double dy) {
        for (Shape member : members) {
            member.moveBy(dx, dy);
        }
        changed();
    }

    @Override
    public void scale(double factor, Point origin) {
        positive(factor, "the scale factor");
        for (Shape member : members) {
            member.scale(factor, origin);
        }
        changed();
    }

    @Override
    public boolean contains(Point p) {
        return members.stream().anyMatch(member -> member.contains(p));
    }

    @Override
    public double area() {
        return members.stream().mapToDouble(Shape::area).sum();
    }

    @Override
    public double perimeter() {
        return members.stream().mapToDouble(Shape::perimeter).sum();
    }

    @Override
    public void restyle(UnaryOperator<Style> change) {
        for (Shape member : members) {
            member.restyle(change);
        }
        changed();
    }

    @Override
    public Group copy() {
        return withMyId(new Group(members.stream().map(Shape::copy).toList()));
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.visitGroup(this);
    }
}
