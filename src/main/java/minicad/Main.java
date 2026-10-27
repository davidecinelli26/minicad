package minicad;

import minicad.app.Headless;
import minicad.ui.MainWindow;

/**
 * The composition root: the one place where the parts are created and wired together, instead of
 * reaching each other through singletons. With no arguments it opens the editor; with a drawing
 * file it runs headless.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            MainWindow.launch();
        } else {
            System.exit(Headless.run(args, System.out, System.err));
        }
    }
}
