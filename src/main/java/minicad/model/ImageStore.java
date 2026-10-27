package minicad.model;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The Flyweight factory: each image file is read and decoded once, and every {@link Picture} that
 * shows it, copies and undo snapshots included, shares the same pixels.
 */
public final class ImageStore {

    private final Map<Path, BufferedImage> images = new HashMap<>();

    public BufferedImage load(Path file) throws IOException {
        Path key = file.toAbsolutePath().normalize();
        BufferedImage image = images.get(key);
        if (image == null) {
            if (!Files.isRegularFile(key)) {
                throw new IOException("no such file: " + file);
            }
            image = ImageIO.read(key.toFile());
            if (image == null) {
                throw new IOException("not an image MiniCAD can read (PNG, JPEG, GIF or BMP): " + file);
            }
            images.put(key, image);
        }
        return image;
    }

    /** How many different images were loaded. */
    public int size() {
        return images.size();
    }
}
