package minicad.model;

/** A point of the drawing plane. As on screen, y grows downwards. */
public record Point(double x, double y) {

    public Point plus(double dx, double dy) {
        return new Point(x + dx, y + dy);
    }

    public double distance(Point p) {
        return Math.hypot(x - p.x, y - p.y);
    }

    /** This point after scaling the plane by {@code k} about {@code origin}. */
    public Point scaled(double k, Point origin) {
        return new Point(origin.x + (x - origin.x) * k, origin.y + (y - origin.y) * k);
    }

    /** {@code (x,y)}, as written in the command language. */
    @Override
    public String toString() {
        return "(" + Numbers.exact(x) + "," + Numbers.exact(y) + ")";
    }
}
