package minicad.app;

/** Where the editor writes: the console of the window, or the terminal when headless. */
public interface Output {

    void print(String text);

    void error(Diagnostic diagnostic);
}
