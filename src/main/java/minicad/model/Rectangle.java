package minicad.model;

public final class Rectangle extends Figure {

    private Point center;
    private double width;
    private double height;

    public Rectangle(Point center, double width, double height, Style style) {
        super(style);
        this.center = center;
        this.width = positive(width, "the width");
        this.height = positive(height, "the height");
    }

    @Override
    public String kind() {
        return "rectangle";
    }

    @Override
    public Point center() {
        return center;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    @Override
    public Bounds bounds() {
        return Bounds.around(center, width, height);
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
        width *= factor;
        height *= factor;
        changed();
    }

    @Override
    public boolean contains(Point p) {
        return bounds().contains(p);
    }

    @Override
    public double area() {
        return width * height;
    }

    @Override
    public double perimeter() {
        return 2 * (width + height);
    }

    @Override
    public Rectangle copy() {
        return withMyId(new Rectangle(center, width, height, style()));
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.visitRectangle(this);
    }
}
