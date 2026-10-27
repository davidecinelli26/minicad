package minicad.model;

import java.util.HexFormat;

/** A colour, 0xRRGGBB; written {@code #rrggbb} or {@code #rgb} in the command language. */
public record Rgb(int value) {

    public Rgb {
        if (value < 0 || value > 0xFFFFFF) {
            throw new IllegalArgumentException("not a colour: " + value);
        }
    }

    /** Parses {@code #rrggbb} or the short form {@code #rgb}. */
    public static Rgb parse(String text) {
        if (!text.matches("#(\\p{XDigit}{3}|\\p{XDigit}{6})")) {
            throw new IllegalArgumentException("not a colour: " + text + " (expected #rgb or #rrggbb)");
        }
        String hex = text.substring(1);
        if (hex.length() == 3) {
            hex = hex.replaceAll("(.)", "$1$1");
        }
        return new Rgb(HexFormat.fromHexDigits(hex));
    }

    public String hex() {
        return "#" + HexFormat.of().toHexDigits(value).substring(2);
    }

    @Override
    public String toString() {
        return hex();
    }
}
