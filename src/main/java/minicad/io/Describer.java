package minicad.io;

import minicad.model.Circle;
import minicad.model.Group;
import minicad.model.Numbers;
import minicad.model.Picture;
import minicad.model.Rectangle;
import minicad.model.Shape;
import minicad.model.ShapeVisitor;
import minicad.model.Style;

import java.util.ArrayList;
import java.util.List;

/** The lines {@code ls} prints for a shape; a group lists its members, indented (Visitor). */
public final class Describer implements ShapeVisitor<Void> {

    private final List<String> lines = new ArrayList<>();
    private String indent = "";

    private Describer() {
    }

    public static List<String> describe(Shape shape) {
        Describer describer = new Describer();
        shape.accept(describer);
        return describer.lines;
    }

    @Override
    public Void visitCircle(Circle circle) {
        line(circle, "radius " + Numbers.brief(circle.radius()) + colours(circle.style()));
        return null;
    }

    @Override
    public Void visitRectangle(Rectangle rectangle) {
        line(rectangle, "size " + size(rectangle.width(), rectangle.height()) + colours(rectangle.style()));
        return null;
    }

    @Override
    public Void visitPicture(Picture picture) {
        line(picture, "size " + size(picture.width(), picture.height()) + "  "
                + ScriptWriter.quote(picture.source()) + colours(picture.style()));
        return null;
    }

    @Override
    public Void visitGroup(Group group) {
        int count = group.members().size();
        line(group, count + (count == 1 ? " member" : " members"));
        String outer = indent;
        indent = outer + "  ";
        group.members().forEach(member -> member.accept(this));
        indent = outer;
        return null;
    }

    private void line(Shape shape, String details) {
        lines.add(indent + shape.id() + "  " + shape.kind() + "  at " + brief(shape) + "  " + details);
    }

    private static String brief(Shape shape) {
        return "(" + Numbers.brief(shape.center().x()) + "," + Numbers.brief(shape.center().y()) + ")";
    }

    private static String size(double width, double height) {
        return Numbers.brief(width) + "x" + Numbers.brief(height);
    }

    private static String colours(Style style) {
        String text = "";
        if (style.fill() != null) {
            text += "  fill " + style.fill().hex();
        }
        if (style.stroke() != null) {
            text += "  stroke " + style.stroke().hex();
        }
        return text;
    }
}
