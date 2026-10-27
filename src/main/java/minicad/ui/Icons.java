package minicad.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/** Line icons drawn in code, in the colour of the text, so they follow the theme and need no files. */
final class Icons {

    private static final int SIZE = 18;

    private Icons() {
    }

    /** An icon drawn on a 24×24 grid. */
    private record Line(Consumer<Graphics2D> painter) implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(SIZE / 24.0, SIZE / 24.0);
            g2.setColor(c != null && !c.isEnabled() ? Theme.muted() : Theme.foreground());
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            painter.accept(g2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }

    private static Path2D path(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        return p;
    }

    static Icon select() {
        return new Line(g -> {
            Path2D arrow = path(6, 3, 6, 19, 10, 15, 13, 21, 15.5, 20, 12.5, 14, 18, 14);
            arrow.closePath();
            g.draw(arrow);
        });
    }

    static Icon circle() {
        return new Line(g -> g.draw(new Ellipse2D.Double(4, 4, 16, 16)));
    }

    static Icon rectangle() {
        return new Line(g -> g.draw(new RoundRectangle2D.Double(3, 6, 18, 12, 2, 2)));
    }

    static Icon pan() {
        return new Line(g -> {
            g.draw(path(12, 3, 12, 21));
            g.draw(path(3, 12, 21, 12));
            g.draw(path(9, 6, 12, 3, 15, 6));
            g.draw(path(9, 18, 12, 21, 15, 18));
            g.draw(path(6, 9, 3, 12, 6, 15));
            g.draw(path(18, 9, 21, 12, 18, 15));
        });
    }

    static Icon undo() {
        return new Line(Icons::undoArrow);
    }

    static Icon redo() {
        return new Line(g -> {
            g.translate(24, 0);
            g.scale(-1, 1);
            undoArrow(g);
        });
    }

    private static void undoArrow(Graphics2D g) {
        g.draw(path(9, 5, 4, 10, 9, 15));
        Path2D p = new Path2D.Double();
        p.moveTo(4, 10);
        p.lineTo(14, 10);
        p.curveTo(18, 10, 20, 13, 20, 15.5);
        p.curveTo(20, 18, 18, 20, 14, 20);
        p.lineTo(11, 20);
        g.draw(p);
    }

    static Icon copy() {
        return new Line(g -> {
            g.draw(new RoundRectangle2D.Double(8, 8, 12, 12, 3, 3));
            g.draw(path(16, 8, 16, 6, 16, 5.5));
            Path2D back = path(16, 5, 16, 4, 4, 4, 4, 16, 5, 16);
            g.draw(back);
        });
    }

    static Icon delete() {
        return new Line(g -> {
            g.draw(path(4, 7, 20, 7));
            g.draw(path(10, 4, 14, 4));
            g.draw(path(6, 7, 7, 20, 17, 20, 18, 7));
            g.draw(path(10, 11, 10, 16));
            g.draw(path(14, 11, 14, 16));
        });
    }

    static Icon group() {
        return new Line(g -> {
            g.draw(new Ellipse2D.Double(5, 5, 8, 8));
            g.draw(new RoundRectangle2D.Double(11, 11, 8, 8, 1.5, 1.5));
            float[] dash = {2f, 2.5f};
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, dash, 0));
            g.draw(new RoundRectangle2D.Double(2, 2, 20, 20, 4, 4));
        });
    }

    static Icon ungroup() {
        return new Line(g -> {
            g.draw(new Ellipse2D.Double(3, 3, 8, 8));
            g.draw(new RoundRectangle2D.Double(13, 13, 8, 8, 1.5, 1.5));
            g.draw(path(14, 4, 20, 10));
            g.draw(path(20, 4, 14, 10));
        });
    }

    static Icon fit() {
        return new Line(g -> {
            g.draw(path(4, 9, 4, 4, 9, 4));
            g.draw(path(15, 4, 20, 4, 20, 9));
            g.draw(path(20, 15, 20, 20, 15, 20));
            g.draw(path(9, 20, 4, 20, 4, 15));
            g.draw(new Ellipse2D.Double(9, 9, 6, 6));
        });
    }

    static Icon grid() {
        return new Line(g -> {
            g.draw(new RoundRectangle2D.Double(3, 3, 18, 18, 3, 3));
            g.draw(path(9, 3, 9, 21));
            g.draw(path(15, 3, 15, 21));
            g.draw(path(3, 9, 21, 9));
            g.draw(path(3, 15, 21, 15));
        });
    }

    static Icon theme() {
        return new Line(g -> {
            Path2D moon = new Path2D.Double();
            moon.moveTo(20, 14.5);
            moon.curveTo(15, 17, 8.5, 13.5, 9.5, 7);
            moon.curveTo(9.8, 5.5, 10.5, 4.3, 11.5, 3.5);
            moon.curveTo(6.5, 4, 3, 8, 3.5, 13);
            moon.curveTo(4, 17.5, 8, 21, 12.5, 20.8);
            moon.curveTo(15.8, 20.6, 18.6, 18.3, 20, 14.5);
            g.draw(moon);
        });
    }

    static Icon fill() {
        return new Line(g -> {
            Path2D bucket = path(5, 11, 11, 5, 18, 12, 12, 18);
            bucket.closePath();
            g.draw(bucket);
            g.draw(path(5, 11, 18, 12));
            g.draw(path(8, 2.5, 11, 5));
            g.fill(new Ellipse2D.Double(18.5, 15, 3.5, 5));
        });
    }

    static Icon outline() {
        return new Line(g -> {
            float[] dash = {3f, 2.6f};
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, dash, 0));
            g.draw(new RoundRectangle2D.Double(4, 4, 16, 16, 3, 3));
        });
    }

    /** A colour swatch, or a crossed box for "none". */
    static Icon swatch(Color colour) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D box = new RoundRectangle2D.Double(x + 1, y + 1, 16, 16, 5, 5);
                if (colour != null) {
                    g2.setColor(colour);
                    g2.fill(box);
                }
                g2.setColor(Theme.muted());
                g2.draw(box);
                if (colour == null) {
                    g2.draw(path(x + 4, y + 14, x + 14, y + 4));
                }
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return SIZE;
            }

            @Override
            public int getIconHeight() {
                return SIZE;
            }
        };
    }

    /** The application icon: a teal circle over a sand square, on a dark tile. */
    static BufferedImage logo(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(size / 64.0, size / 64.0);
        g.setColor(new Color(0x264653));
        g.fill(new RoundRectangle2D.Double(2, 2, 60, 60, 16, 16));
        g.setColor(new Color(0xe9c46a));
        g.fill(new RoundRectangle2D.Double(28, 28, 24, 24, 4, 4));
        g.setColor(new Color(0x2a9d8f));
        g.fill(new Ellipse2D.Double(12, 12, 28, 28));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(12, 12, 28, 28));
        g.dispose();
        return image;
    }
}
