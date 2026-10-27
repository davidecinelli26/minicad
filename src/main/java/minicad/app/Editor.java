package minicad.app;

import minicad.command.Command;
import minicad.command.CommandException;
import minicad.command.History;
import minicad.command.Workspace;
import minicad.lang.Parser;
import minicad.lang.ScriptError;
import minicad.lang.Statement;
import minicad.model.Drawing;
import minicad.model.ImageStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The Facade of MiniCAD: one object that takes a line of text and does everything behind it. It
 * parses the line (Interpreter), turns it into a command and runs it through the history (Command,
 * Memento) on the drawing (Composite, Observer). The window, the headless runner and the tests all
 * use MiniCAD through this class.
 * <p>
 * The editor also replaces the singletons of the 2024 version: there is one drawing per editor,
 * created here and passed on, so tests can build as many editors as they like.
 */
public final class Editor implements Workspace {

    private final Drawing drawing = new Drawing();
    private final History history = new History();
    private final ImageStore images = new ImageStore();
    private final Output output;
    private final Path folder;
    private boolean quiet;

    /** An editor whose relative paths start from the working directory. */
    public Editor(Output output) {
        this(output, Path.of(""));
    }

    public Editor(Output output, Path folder) {
        this.output = Objects.requireNonNull(output);
        this.folder = folder.toAbsolutePath();
    }

    /**
     * Runs one line of the command language. A mistake is reported to the output and leaves the
     * drawing as it was.
     *
     * @return whether the line ran without errors
     */
    public boolean run(String line) {
        try {
            Optional<Statement> statement = Parser.parse(line);
            if (statement.isPresent()) {
                history.run(statement.get().interpret(this), line.strip(), this);
            }
            return true;
        } catch (ScriptError e) {
            output.error(new Diagnostic(e.source() != null ? e.source() : line, e.span(), e.getMessage(), e.hint()));
        } catch (CommandException | IllegalArgumentException | IllegalStateException e) {
            output.error(new Diagnostic(line, null, e.getMessage(), null));
        }
        return false;
    }

    /** Runs a command built by code rather than typed, as one undoable step with the given label. */
    public boolean run(Command command, String label) {
        try {
            history.run(command, label, this);
            return true;
        } catch (ScriptError e) {
            output.error(new Diagnostic(e.source(), e.span(), e.getMessage(), e.hint()));
        } catch (CommandException | IllegalArgumentException | IllegalStateException e) {
            output.error(new Diagnostic(label, null, e.getMessage(), null));
        }
        return false;
    }

    @Override
    public void open(Path script) {
        List<String> lines;
        try {
            lines = Files.readAllLines(script);
        } catch (IOException e) {
            throw new CommandException("cannot read " + script + (Files.exists(script) ? "" : ": no such file"), e);
        }
        replace(String.valueOf(script.getFileName()), lines);
    }

    /**
     * Replaces the drawing with the one the lines build. Edits run quietly; listings still print.
     * Called inside a history step, so a mistake on any line undoes the whole thing.
     */
    public void replace(String name, List<String> lines) {
        drawing.clear();
        for (int i = 0; i < lines.size(); i++) {
            String where = name + ", line " + (i + 1);
            String line = lines.get(i);
            try {
                Optional<Statement> statement = Parser.parse(line);
                if (statement.isEmpty()) {
                    continue;
                }
                if (statement.get() instanceof Statement.Undo || statement.get() instanceof Statement.Redo
                        || statement.get() instanceof Statement.Load) {
                    throw new ScriptError("undo, redo and load cannot be used inside a file", statement.get().span());
                }
                Command command = statement.get().interpret(this);
                quiet = command.isEdit();
                command.execute(this);
            } catch (ScriptError e) {
                throw e.source() != null ? e : e.at(where, line);
            } catch (CommandException | IllegalArgumentException | IllegalStateException e) {
                throw new ScriptError(where + ": " + e.getMessage(), null, null, line);
            } finally {
                quiet = false;
            }
        }
    }

    @Override
    public Drawing drawing() {
        return drawing;
    }

    @Override
    public History history() {
        return history;
    }

    @Override
    public ImageStore images() {
        return images;
    }

    @Override
    public Path resolve(String path) {
        return folder.resolve(path).normalize();
    }

    @Override
    public void print(String text) {
        if (!quiet) {
            output.print(text);
        }
    }
}
