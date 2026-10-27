package minicad.io;

import minicad.model.Bounds;
import minicad.model.Circle;
import minicad.model.Drawing;
import minicad.model.Group;
import minicad.model.Numbers;
import minicad.model.Picture;
import minicad.model.Rectangle;
import minicad.model.Rgb;
import minicad.model.Shape;
import minicad.model.ShapeVisitor;
import minicad.model.Style;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;

/** Exports a drawing as a standalone SVG file, images embedded (Visitor). */
public final class SvgWriter implements ShapeVisitor<Void> {

    /** Width of the outlines, in drawing units, here and on the canvas. */
    public static final double STROKE_WIDTH = 2;

    private static final double MARGIN = 20;
    private static final String PAPER = "#ffffff";

    private final StringBuilder svg = new StringBuilder();
    private String indent = "  ";

    private SvgWriter() {
    }

    public static String write(Drawing drawing) {
        Bounds box = drawing.bounds().map(b -> b.grow(MARGIN)).orElse(new Bounds(0, 0, 400, 300));
        String x = n(box.minX()), y = n(box.minY()), w = n(box.width()), h = n(box.height());
        SvgWriter writer = new SvgWriter();
        writer.svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"")
                .append(x).append(' ').append(y).append(' ').append(w).append(' ').append(h)
                .append("\" width=\"").append(w).append("\" height=\"").append(h).append("\">\n");
        writer.svg.append("  <rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(w)
                .append("\" height=\"").append(h).append("\" fill=\"").append(PAPER).append("\"/>\n");
        drawing.items().forEach(shape -> shape.accept(writer));
        writer.svg.append("</svg>\n");
        return writer.svg.toString();
    }

    @Override
    public Void visitCircle(Circle circle) {
        element(circle, "circle", "cx=\"" + n(circle.center().x()) + "\" cy=\"" + n(circle.center().y())
                + "\" r=\"" + n(circle.radius()) + "\"", circle.style());
        return null;
    }

    @Override
    public Void visitRectangle(Rectangle rectangle) {
        element(rectangle, "rect", box(rectangle.bounds()), rectangle.style());
        return null;
    }

    @Override
    public Void visitPicture(Picture picture) {
        svg.append(indent).append("<image id=\"").append(picture.id()).append("\" ").append(box(picture.bounds()))
                .append(" preserveAspectRatio=\"none\" href=\"data:image/png;base64,").append(png(picture))
                .append("\"/>\n");
        if (picture.style().stroke() != null) {
            svg.append(indent).append("<rect ").append(box(picture.bounds())).append(paint(picture.style()))
                    .append("/>\n");
        }
        return null;
    }

    @Override
    public Void visitGroup(Group group) {
        svg.append(indent).append("<g id=\"").append(group.id()).append("\">\n");
        String outer = indent;
        indent = outer + "  ";
        group.members().forEach(member -> member.accept(this));
        indent = outer;
        svg.append(indent).append("</g>\n");
        return null;
    }

    private void element(Shape shape, String tag, String geometry, Style style) {
        svg.append(indent).append('<').append(tag).append(" id=\"").append(shape.id()).append("\" ")
                .append(geometry).append(paint(style)).append("/>\n");
    }

    private static String paint(Style style) {
        return " fill=\"" + colour(style.fill()) + "\" stroke=\"" + colour(style.stroke()) + "\""
                + (style.stroke() == null ? "" : " stroke-width=\"" + n(STROKE_WIDTH) + "\"");
    }

    private static String colour(Rgb rgb) {
        return rgb == null ? "none" : rgb.hex();
    }

    private static String box(Bounds b) {
        return "x=\"" + n(b.minX()) + "\" y=\"" + n(b.minY()) + "\" width=\"" + n(b.width())
                + "\" height=\"" + n(b.height()) + "\"";
    }

    private static String png(Picture picture) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(picture.image(), "png", bytes);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String n(double value) {
        return Numbers.exact(value);
    }
}
