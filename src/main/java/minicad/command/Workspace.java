package minicad.command;

import minicad.model.Drawing;
import minicad.model.ImageStore;

import java.nio.file.Path;

/**
 * What a command can reach. The editor implements it; commands see only this narrow interface, so
 * the command layer does not depend on the parser or on the user interface.
 */
public interface Workspace {

    Drawing drawing();

    History history();

    ImageStore images();

    /** A path as the user typed it, resolved against the working folder. */
    Path resolve(String path);

    /** Shows a line of output: in the console, or on standard output when headless. */
    void print(String text);

    /** Replaces the drawing with the one a script file builds. */
    void open(Path script);
}
