package minicad.model;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapeTest {

    static final double EPS = 1e-9;

    static Circle circle(double x, double y, double r) {
        return new Circle(new Point(x, y), r, Style.OUTLINE);
    }

    static Rectangle rectangle(double x, double y, double w, double h) {
        return new Rectangle(new Point(x, y), w, h, Style.OUTLINE);
    }

    @Test
    void circleMeasures() {
        Circle c = circle(30, 30, 5);
        assertEquals(Math.PI * 25, c.area(), EPS);
        assertEquals(2 * Math.PI * 5, c.perimeter(), EPS);
        assertEquals(new Bounds(25, 25, 35, 35), c.bounds());
        assertTrue(c.contains(new Point(33, 33)));
        assertFalse(c.contains(new Point(34, 34)));
    }

    @Test
    void rectangleMeasures() {
        Rectangle r = rectangle(50, 40, 100, 20);
        assertEquals(2000, r.area(), EPS);
        assertEquals(240, r.perimeter(), EPS);
        assertEquals(new Bounds(0, 30, 100, 50), r.bounds());
    }

    @Test
    void scaledPictureMeasuresTheScaledSize() {
        // The 2024 version measured the image file, ignoring the scale.
        Picture p = new Picture("tile.png", new BufferedImage(40, 10, BufferedImage.TYPE_INT_ARGB),
                new Point(0, 0), 1, Style.NONE);
        p.scale(2);
        assertEquals(80 * 20, p.area(), EPS);
        assertEquals(2 * (80 + 20), p.perimeter(), EPS);
    }

    @Test
    void scalingAboutAPointMovesTheCentreToo() {
        Circle c = circle(10, 0, 2);
        c.scale(3, new Point(0, 0));
        assertEquals(new Point(30, 0), c.center());
        assertEquals(6, c.radius(), EPS);
    }

    @Test
    void sizesAndFactorsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> circle(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> rectangle(0, 0, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> circle(0, 0, 1).scale(0));
    }

    @Test
    void copyIsIndependentButKeepsIdAndSharesPixels() {
        BufferedImage pixels = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Picture p = new Picture("a.png", pixels, new Point(1, 1), 1, Style.NONE);
        p.setId("obj7");
        Picture copy = p.copy();
        copy.moveBy(10, 0);
        assertEquals("obj7", copy.id());
        assertEquals(new Point(1, 1), p.center());
        assertSame(pixels, copy.image(), "the flyweight pixels are shared");
        assertNotSame(p, copy);
    }

    @Test
    void listenersHearChanges() {
        Circle c = circle(0, 0, 1);
        int[] heard = {0};
        c.addListener(shape -> heard[0]++);
        c.moveBy(1, 1);
        c.scale(2);
        c.restyle(style -> style.withFill(Rgb.parse("#fff")));
        assertEquals(3, heard[0]);
    }

    @Test
    void coloursParseAndPrint() {
        assertEquals("#aabbcc", Rgb.parse("#abc").hex());
        assertEquals("#2a9d8f", Rgb.parse("#2A9D8F").hex());
        assertThrows(IllegalArgumentException.class, () -> Rgb.parse("#12"));
    }

    @Test
    void numbersAreShortAndStable() {
        assertEquals("120", Numbers.exact(120.0));
        assertEquals("0.3333", Numbers.exact(1.0 / 3));
        assertEquals("0", Numbers.exact(-0.00001));
        assertEquals("3.14", Numbers.brief(Math.PI));
        assertEquals("(1.5,-2)", new Point(1.5, -2).toString());
    }
}
