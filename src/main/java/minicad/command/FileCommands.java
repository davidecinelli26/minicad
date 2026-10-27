package minicad.command;

import minicad.io.ScriptWriter;
import minicad.io.SvgWriter;
import minicad.model.Drawing;
import minicad.model.Group;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

/** {@code save}, {@code load} and {@code export}. */
public final class FileCommands {

    private FileCommands() {
    }

    /** {@code save}: writes the drawing as a script; it does not change the drawing. */
    public static Query save(Path file) {
        return workspace -> write(workspace, file, ScriptWriter::write, "saved");
    }

    /** {@code export}: writes the drawing as an SVG image. */
    public static Query export(Path file) {
        return workspace -> write(workspace, file, SvgWriter::write, "exported");
    }

    /** {@code load}: replaces the drawing with a saved one. It is an edit, so it can be undone. */
    public static Command load(Path file) {
        return workspace -> {
            workspace.open(file);
            workspace.print("loaded " + file.getFileName() + ": " + count(workspace.drawing()));
        };
    }

    private static void write(Workspace workspace, Path file, Function<Drawing, String> format, String verb) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, format.apply(workspace.drawing()));
        } catch (IOException e) {
            throw new CommandException("cannot write " + file + ": " + e.getMessage(), e);
        }
        workspace.print(verb + " " + file.getFileName() + ": " + count(workspace.drawing()));
    }

    /** {@code 41 shapes in 12 groups} */
    private static String count(Drawing drawing) {
        long groups = drawing.shapes().filter(shape -> shape instanceof Group).count();
        long figures = drawing.shapes().count() - groups;
        return figures + (figures == 1 ? " shape" : " shapes")
                + (groups == 0 ? "" : " in " + groups + (groups == 1 ? " group" : " groups"));
    }
}
