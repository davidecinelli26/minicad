package minicad.model;

/** How a figure is painted. A null colour means none: no fill, or no outline. */
public record Style(Rgb fill, Rgb stroke) {

    /** New circles and rectangles: outline only, slate grey. */
    public static final Style OUTLINE = new Style(null, Rgb.parse("#334155"));

    /** New images: no frame. */
    public static final Style NONE = new Style(null, null);

    public Style withFill(Rgb fill) {
        return new Style(fill, stroke);
    }

    public Style withStroke(Rgb stroke) {
        return new Style(fill, stroke);
    }
}
