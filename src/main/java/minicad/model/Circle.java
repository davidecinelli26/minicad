package minicad.model;

public final class Circle extends Figure {

    private Point center;
    private double radius;

    public Circle(Point center, double radius, Style style) {
        super(style);
        this.center = center;
        this.radius = positive(radius, "the radius");
    }

    @Override
    public String kind() {
        return "circle";
    }

    @Override
    public Point center() {
        return center;
    }

    public double radius() {
        return radius;
    }

    @Override
    public Bounds bounds() {
        return Bounds.around(center, 2 * radius, 2 * radius);
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
        radius *= factor;
        changed();
    }

    @Override
    public boolean contains(Point p) {
        return center.distance(p) <= radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public double perimeter() {
        return 2 * Math.PI * radius;
    }

    @Override
    public Circle copy() {
        return withMyId(new Circle(center, radius, style()));
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.visitCircle(this);
    }
}
