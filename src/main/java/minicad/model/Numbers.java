package minicad.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** How numbers are written: in scripts, and to people. */
public final class Numbers {

    private Numbers() {
    }

    /** Up to four decimals, no trailing zeros: what scripts and SVG files store. */
    public static String exact(double value) {
        return format(value, 4);
    }

    /** Up to two decimals, no trailing zeros: what the console shows. */
    public static String brief(double value) {
        return format(value, 2);
    }

    private static String format(double value, int decimals) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("not a finite number: " + value);
        }
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP);
        if (rounded.signum() == 0) {
            return "0";
        }
        return rounded.stripTrailingZeros().toPlainString();
    }
}
