package minicad.ui;

import minicad.app.Diagnostic;
import minicad.app.Output;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The console: the log of everything that happened, typed or done with the mouse, and the line to
 * type commands in. Up and down walk the history of typed lines, Tab completes, Escape clears.
 */
final class ConsolePanel extends JPanel implements Output {

    private static final int MAX_LOG = 200_000;

    private final JTextPane log = new JTextPane();
    private final JTextField input = new JTextField();
    private final List<String> typed = new ArrayList<>();
    private int recall;
    private String lastEcho;
    private final Predicate<String> runner;
    private final Supplier<Collection<String>> ids;

    /**
     * @param runner runs a line and says whether it worked
     * @param ids    the ids in the drawing, for completion
     */
    ConsolePanel(Predicate<String> runner, Supplier<Collection<String>> ids) {
        super(new BorderLayout());
        this.runner = runner;
        this.ids = ids;
        log.setEditable(false);
        log.setFont(Theme.mono(13f));
        log.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        input.setFont(Theme.mono(14f));
        input.putClientProperty("JTextField.placeholderText",
                "Type a command: help lists them, Tab completes, ↑↓ recall");
        input.putClientProperty("JTextField.showClearButton", true);
        JLabel prompt = new JLabel("›");
        prompt.setFont(Theme.mono(18f));
        prompt.setForeground(Theme.ACCENT);
        prompt.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 2));
        input.putClientProperty("JTextField.leadingComponent", prompt);
        input.setFocusTraversalKeysEnabled(false);
        bind("ENTER", this::submit);
        bind("UP", () -> recall(-1));
        bind("DOWN", () -> recall(1));
        bind("TAB", this::complete);
        bind("ESCAPE", () -> input.setText(""));
        JPanel line = new JPanel(new BorderLayout());
        line.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
        line.add(input, BorderLayout.CENTER);
        add(line, BorderLayout.SOUTH);
    }

    JTextField input() {
        return input;
    }

    private void bind(String key, Runnable action) {
        input.getInputMap().put(KeyStroke.getKeyStroke(key), key);
        input.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void submit() {
        String line = input.getText();
        if (line.isBlank()) {
            return;
        }
        if (typed.isEmpty() || !typed.getLast().equals(line)) {
            typed.add(line);
        }
        recall = typed.size();
        input.setText("");
        runner.test(line);
    }

    private void recall(int step) {
        if (typed.isEmpty()) {
            return;
        }
        recall = Math.clamp(recall + step, 0, typed.size());
        input.setText(recall == typed.size() ? "" : typed.get(recall));
    }

    private void complete() {
        Completer.Result result = Completer.complete(input.getText(), ids.get());
        input.setText(result.line());
        if (!result.choices().isEmpty()) {
            append(String.join("   ", result.choices()) + "\n", Theme.muted(), false);
        }
    }

    /** Shows a command about to run, typed or made by the mouse. */
    void echo(String command) {
        lastEcho = command;
        append("› ", Theme.ACCENT, true);
        append(command + "\n", UIManager.getColor("Label.foreground"), true);
    }

    @Override
    public void print(String text) {
        append(text + "\n", UIManager.getColor("Label.foreground"), false);
    }

    /** A remark of the editor itself, in grey. */
    void note(String text) {
        append(text + "\n", Theme.muted(), false);
    }

    @Override
    public void error(Diagnostic diagnostic) {
        if (diagnostic.span() != null && diagnostic.line() != null) {
            int start = diagnostic.span().start();
            int width = Math.max(1, diagnostic.span().end() - start);
            if (diagnostic.line().equals(lastEcho)) {
                // Underline the command as echoed above, after its two-character prompt.
                append("  " + " ".repeat(start) + "^".repeat(width) + " ", Theme.ERROR, true);
            } else {
                // A line of a file: show it first.
                append("  " + diagnostic.line() + "\n", Theme.muted(), false);
                append("  " + " ".repeat(start) + "^".repeat(width) + " ", Theme.ERROR, true);
            }
            append(diagnostic.message() + "\n", Theme.ERROR, false);
        } else {
            append(diagnostic.message() + "\n", Theme.ERROR, false);
        }
        if (diagnostic.hint() != null) {
            append("  usage: " + diagnostic.hint() + "\n", Theme.muted(), false);
        }
    }

    private void append(String text, Color colour, boolean bold) {
        StyledDocument doc = log.getStyledDocument();
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, colour);
        StyleConstants.setBold(style, bold);
        try {
            doc.insertString(doc.getLength(), text, style);
            if (doc.getLength() > MAX_LOG) {
                doc.remove(0, doc.getLength() - MAX_LOG);
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        log.setCaretPosition(doc.getLength());
    }
}
