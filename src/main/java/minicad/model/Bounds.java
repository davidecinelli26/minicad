package minicad.model;

/** An axis-aligned box: the extent of a shape or of a whole drawing. */
public record Bounds(double minX, double minY, double maxX, double maxY) {

    public Bounds {
        if (minX > maxX || minY > maxY) {
            throw new IllegalArgumentException("empty bounds");
        }
    }

    /** The box of the given size centred on {@code center}. */
    public static Bounds around(Point center, double width, double height) {
        return new Bounds(center.x() - width / 2, center.y() - height / 2,
                center.x() + width / 2, center.y() + height / 2);
    }

    public double width() {
        return maxX - minX;
    }

    public double height() {
        return maxY - minY;
    }

    public Point center() {
        return new Point((minX + maxX) / 2, (minY + maxY) / 2);
    }

    public Bounds union(Bounds other) {
        return new Bounds(Math.min(minX, other.minX), Math.min(minY, other.minY),
                Math.max(maxX, other.maxX), Math.max(maxY, other.maxY));
    }

    public Bounds grow(double margin) {
        return new Bounds(minX - margin, minY - margin, maxX + margin, maxY + margin);
    }

    public boolean contains(Point p) {
        return p.x() >= minX && p.x() <= maxX && p.y() >= minY && p.y() <= maxY;
    }
}
