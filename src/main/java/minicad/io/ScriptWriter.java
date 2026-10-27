package minicad.io;

import minicad.model.Circle;
import minicad.model.Drawing;
import minicad.model.Group;
import minicad.model.Numbers;
import minicad.model.Picture;
import minicad.model.Rectangle;
import minicad.model.Shape;
import minicad.model.ShapeVisitor;
import minicad.model.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Saves a drawing as a script of the command language itself (Visitor), so a saved file can be
 * read, edited and run by hand. Loading the script into an empty drawing builds the same shapes
 * with ids in the same order; a group is written after its members, with the ids they will get.
 */
public final class ScriptWriter implements ShapeVisitor<String> {

    private final List<String> lines = new ArrayList<>();
    private int figures;
    private int groups;

    private ScriptWriter() {
    }

    public static String write(Drawing drawing) {
        ScriptWriter writer = new ScriptWriter();
        writer.lines.add("// MiniCAD drawing: load it with  load \"file.mcad\"");
        drawing.items().forEach(shape -> shape.accept(writer));
        return String.join("\n", writer.lines) + "\n";
    }

    @Override
    public String visitCircle(Circle circle) {
        lines.add("new circle (" + Numbers.exact(circle.radius()) + ") " + circle.center()
                + style(circle.style(), Style.OUTLINE));
        return nextFigure();
    }

    @Override
    public String visitRectangle(Rectangle rectangle) {
        lines.add("new rectangle " + rectangle.center() + " (" + Numbers.exact(rectangle.width()) + ","
                + Numbers.exact(rectangle.height()) + ")" + style(rectangle.style(), Style.OUTLINE));
        return nextFigure();
    }

    @Override
    public String visitPicture(Picture picture) {
        lines.add("new image " + quote(picture.source()) + " " + picture.center()
                + style(picture.style(), Style.NONE));
        String id = nextFigure();
        if (picture.factor() != 1) {
            lines.add("scale " + id + " " + Numbers.exact(picture.factor()));
        }
        return id;
    }

    @Override
    public String visitGroup(Group group) {
        String members = group.members().stream()
                .map(member -> member.accept(this))
                .collect(Collectors.joining(" "));
        lines.add("grp " + members);
        return "grp" + groups++;
    }

    private String nextFigure() {
        return "obj" + figures++;
    }

    /** The options that differ from the style a new shape of that kind gets. */
    static String style(Style style, Style initial) {
        StringBuilder options = new StringBuilder();
        if (style.fill() != null) {
            options.append(" fill ").append(style.fill().hex());
        }
        if (style.stroke() == null && initial.stroke() != null) {
            options.append(" stroke none");
        } else if (style.stroke() != null && !style.stroke().equals(initial.stroke())) {
            options.append(" stroke ").append(style.stroke().hex());
        }
        return options.toString();
    }

    public static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Ids of the shapes, for the statements that take several. */
    public static String ids(List<? extends Shape> shapes) {
        return shapes.stream().map(Shape::id).collect(Collectors.joining(" "));
    }
}
