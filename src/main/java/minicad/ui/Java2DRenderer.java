package minicad.ui;

import minicad.io.SvgWriter;
import minicad.model.Bounds;
import minicad.model.Circle;
import minicad.model.Group;
import minicad.model.Picture;
import minicad.model.Rectangle;
import minicad.model.Rgb;
import minicad.model.ShapeVisitor;
import minicad.model.Style;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * Paints shapes with Java2D (Visitor). It replaces the view classes and the view factory of the
 * 2024 version: one stateless visitor instead of a class per shape registered in a singleton map.
 * Coordinates are drawing units; the canvas sets the zoom on the graphics before calling it.
 */
final class Java2DRenderer implements ShapeVisitor<Void> {

    private static final BasicStroke OUTLINE = new BasicStroke((float) SvgWriter.STROKE_WIDTH,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private final Graphics2D g;

    Java2DRenderer(Graphics2D g) {
        this.g = g;
    }

    @Override
    public Void visitCircle(Circle circle) {
        Bounds b = circle.bounds();
        paint(new Ellipse2D.Double(b.minX(), b.minY(), b.width(), b.height()), circle.style());
        return null;
    }

    @Override
    public Void visitRectangle(Rectangle rectangle) {
        paint(box(rectangle.bounds()), rectangle.style());
        return null;
    }

    @Override
    public Void visitPicture(Picture picture) {
        Bounds b = picture.bounds();
        AffineTransform place = AffineTransform.getTranslateInstance(b.minX(), b.minY());
        place.scale(picture.factor(), picture.factor());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(picture.image(), place, null);
        paint(box(b), new Style(null, picture.style().stroke()));
        return null;
    }

    @Override
    public Void visitGroup(Group group) {
        group.members().forEach(member -> member.accept(this));
        return null;
    }

    private void paint(java.awt.Shape outline, Style style) {
        if (style.fill() != null) {
            g.setColor(colour(style.fill()));
            g.fill(outline);
        }
        if (style.stroke() != null) {
            g.setColor(colour(style.stroke()));
            g.setStroke(OUTLINE);
            g.draw(outline);
        }
    }

    static Color colour(Rgb rgb) {
        return new Color(rgb.value());
    }

    static Rectangle2D box(Bounds b) {
        return new Rectangle2D.Double(b.minX(), b.minY(), b.width(), b.height());
    }
}
