package minicad.app;

import minicad.io.ScriptWriter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MiniCAD without a window: runs a drawing file and can export it. It proves the point of MVC, the
 * model and the commands work with no view at all, and the CI uses it to draw the README image.
 * <pre>
 * java -jar minicad.jar garden.mcad --svg garden.svg
 * </pre>
 */
public final class Headless {

    static final String USAGE = """
            usage: java -jar minicad.jar                         open the editor
                   java -jar minicad.jar FILE.mcad [--svg OUT.svg]  run a drawing file, optionally export it""";

    private Headless() {
    }

    /** Runs the arguments; returns the exit code: 0 done, 1 an error in the drawing, 2 bad arguments. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        List<String> files = new ArrayList<>();
        String svg = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h", "--help" -> {
                    out.println(USAGE);
                    return 0;
                }
                case "--svg" -> {
                    if (i + 1 == args.length) {
                        err.println("--svg needs the name of the file to write\n" + USAGE);
                        return 2;
                    }
                    svg = args[++i];
                }
                default -> {
                    if (args[i].startsWith("-")) {
                        err.println("unknown option " + args[i] + "\n" + USAGE);
                        return 2;
                    }
                    files.add(args[i]);
                }
            }
        }
        if (files.size() != 1) {
            err.println(USAGE);
            return 2;
        }
        Editor editor = new Editor(new Output() {
            @Override
            public void print(String text) {
                out.println(text);
            }

            @Override
            public void error(Diagnostic diagnostic) {
                err.println(diagnostic.render());
            }
        });
        boolean ok = editor.run("load " + ScriptWriter.quote(files.getFirst()))
                && (svg == null || editor.run("export " + ScriptWriter.quote(svg)));
        return ok ? 0 : 1;
    }
}
