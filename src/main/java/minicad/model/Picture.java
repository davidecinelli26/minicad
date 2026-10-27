package minicad.model;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * An image file placed on the drawing. The pixels are the intrinsic state of a Flyweight: they come
 * from an {@link ImageStore} and every copy of the picture shares them, while position and scale
 * are the extrinsic state each picture keeps.
 */
public final class Picture extends Figure {

    private final String source;
    private final BufferedImage image;
    private Point center;
    private double factor;

    /**
     * @param source the path as written by the user, kept to save the drawing
     * @param image  the decoded pixels, shared
     */
    public Picture(String source, BufferedImage image, Point center, double factor, Style style) {
        super(style);
        this.source = Objects.requireNonNull(source);
        this.image = Objects.requireNonNull(image);
        this.center = center;
        this.factor = positive(factor, "the scale factor");
    }

    @Override
    public String kind() {
        return "image";
    }

    public String source() {
        return source;
    }

    public BufferedImage image() {
        return image;
    }

    /** How much the picture is enlarged with respect to the image file: 1 is one pixel per unit. */
    public double factor() {
        return factor;
    }

    public double width() {
        return image.getWidth() * factor;
    }

    public double height() {
        return image.getHeight() * factor;
    }

    @Override
    public Point center() {
        return center;
    }

    @Override
    public Bounds bounds() {
        return Bounds.around(center, width(), height());
    }

    @Override
    public void moveBy(double dx, double dy) {
        center = center.plus(dx, dy);
        changed();
    }

    @Override
    public void scale(double factor, Point origin) {
        positive(factor, "the scale factor");
        center = center.scaled(factor, origin);
        this.factor *= factor;
        changed();
    }

    @Override
    public boolean contains(Point p) {
        return bounds().contains(p);
    }

    @Override
    public double area() {
        return width() * height();
    }

    @Override
    public double perimeter() {
        return 2 * (width() + height());
    }

    @Override
    public Picture copy() {
        return withMyId(new Picture(source, image, center, factor, style()));
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.visitPicture(this);
    }
}
