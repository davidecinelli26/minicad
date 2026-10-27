package minicad.ui;

import minicad.app.Editor;
import minicad.io.ScriptWriter;
import minicad.model.Drawing;
import minicad.model.Group;
import minicad.model.Numbers;
import minicad.model.Point;
import minicad.model.Rgb;
import minicad.model.Shape;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The window: canvas, object tree and console around one {@link Editor}. Every button, menu item,
 * key and mouse gesture ends in {@link #run(String)}, which echoes a command line in the console and
 * hands it to the editor. The window never edits the drawing by any other way.
 */
public final class MainWindow extends JFrame {

    private static final List<Rgb> PALETTE = List.of(
            "#264653", "#2a9d8f", "#8ab17d", "#e9c46a", "#f4a261", "#e76f51",
            "#d1495b", "#6d597a", "#457b9d", "#a8dadc", "#ffffff", "#334155").stream().map(Rgb::parse).toList();

    /** The key that picks each tool while the canvas has the focus. */
    private static final Map<String, String> TOOL_KEYS = Map.of("Select", "V", "Circle", "C", "Rectangle", "R", "Pan", "H");

    private final Selection selection = new Selection();
    private final ConsolePanel console;
    private final Editor editor;
    private final Drawing drawing;
    private final Canvas canvas;
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final Map<Tool, AbstractButton> toolButtons = new LinkedHashMap<>();
    private final Map<Tool, JRadioButtonMenuItem> toolItems = new LinkedHashMap<>();
    private final List<JButton> paletteButtons = new ArrayList<>();
    private final JLabel where = status();
    private final JLabel what = status();
    private final JLabel view = status();
    private File folder = new File(System.getProperty("user.dir"));

    private final Action undo = action("Undo", Icons.undo(), "ctrl Z", () -> run("undo"));
    private final Action redo = action("Redo", Icons.redo(), "ctrl Y", () -> run("redo"));
    private final Action duplicate = action("Duplicate", Icons.copy(), "ctrl D", this::duplicate);
    private final Action delete = action("Delete", Icons.delete(), null, this::delete);
    private final Action group = action("Group", Icons.group(), "ctrl G", this::group);
    private final Action ungroup = action("Ungroup", Icons.ungroup(), "ctrl shift G", this::ungroup);
    private final Action fit = action("Zoom to fit", Icons.fit(), "ctrl 0", () -> canvasLater(Canvas::fit));

    public static void launch() {
        System.setProperty("flatlaf.uiScale.allowScaleDown", "true");
        SwingUtilities.invokeLater(() -> {
            Theme.install(false);
            new MainWindow().setVisible(true);
        });
    }

    MainWindow() {
        super("MiniCAD");
        console = new ConsolePanel(this::run, () -> idsInDrawing());
        editor = new Editor(console);
        drawing = editor.drawing();
        canvas = new Canvas(drawing, selection);
        Tool.Context context = new Tool.Context() {
            @Override public Drawing drawing() { return drawing; }
            @Override public Selection selection() { return selection; }
            @Override public boolean run(String command) { return MainWindow.this.run(command); }
            @Override public Point snap(Point p) { return canvas.snap(p); }
            @Override public void panBy(double dx, double dy) { canvas.panBy(dx, dy); }
            @Override public void repaint() { canvas.repaint(); }
        };
        for (Tool tool : List.of(new SelectTool(context), new ShapeTool.CircleTool(context),
                new ShapeTool.RectangleTool(context), new PanTool(context))) {
            tools.put(tool.name(), tool);
        }

        setIconImages(List.of(Icons.logo(16), Icons.logo(32), Icons.logo(64), Icons.logo(128)));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        setJMenuBar(menus());
        add(toolbar(), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
        add(statusBar(), BorderLayout.SOUTH);
        bindCanvasKeys();

        selection.addListener(this::refresh);
        drawing.addListener(change -> refresh());
        canvas.onViewChange(c -> refreshView());
        use(tools.get("Select"));
        refresh();
        console.note("Welcome to MiniCAD. Type help to list the commands, or draw with the tools above.");
        console.note("Everything you do with the mouse appears here as the command that does it.");
    }

    /** Runs a command line: the one door through which the window changes the drawing. */
    boolean run(String command) {
        console.echo(command);
        boolean ok = editor.run(command);
        refresh();
        return ok;
    }

    // ---- layout ----------------------------------------------------------------------------

    private JComponent body() {
        ObjectTree tree = new ObjectTree(drawing, selection);
        tree.setPreferredSize(new Dimension(230, 400));
        JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, canvas);
        top.setDividerLocation(230);
        top.setBorder(null);
        console.setPreferredSize(new Dimension(800, 210));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, console);
        split.setResizeWeight(1);
        split.setBorder(null);
        return split;
    }

    private JToolBar toolbar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        ButtonGroup buttons = new ButtonGroup();
        Map<String, Icon> icons = Map.of("Select", Icons.select(), "Circle", Icons.circle(),
                "Rectangle", Icons.rectangle(), "Pan", Icons.pan());
        for (Tool tool : tools.values()) {
            JToggleButton button = new JToggleButton(icons.get(tool.name()));
            button.setToolTipText(tool.name() + " (" + TOOL_KEYS.get(tool.name()) + ")");
            button.setFocusable(false);
            button.addActionListener(e -> use(tool));
            buttons.add(button);
            bar.add(button);
            toolButtons.put(tool, button);
        }
        bar.addSeparator();
        bar.add(button(undo));
        bar.add(button(redo));
        bar.addSeparator();
        bar.add(button(duplicate));
        bar.add(button(delete));
        bar.add(button(group));
        bar.add(button(ungroup));
        bar.addSeparator();
        bar.add(paletteButton("Fill colour", Icons.fill(), "fill"));
        bar.add(paletteButton("Outline colour", Icons.outline(), "stroke"));
        bar.addSeparator();
        bar.add(button(fit));
        JToggleButton snap = new JToggleButton(Icons.grid(), canvas.isSnapping());
        snap.setToolTipText("Snap to the grid");
        snap.addActionListener(e -> canvas.setSnapping(snap.isSelected()));
        bar.add(snap);
        bar.add(Box.createHorizontalGlue());
        JButton theme = new JButton(Icons.theme());
        theme.setToolTipText("Light or dark theme");
        theme.addActionListener(e -> Theme.toggle());
        bar.add(theme);
        return bar;
    }

    private JMenuBar menus() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(item("New", "ctrl N", () -> run("clear")));
        file.add(item("Open…", "ctrl O", this::open));
        file.add(item("Save…", "ctrl S", this::save));
        file.add(item("Export SVG…", "ctrl E", this::export));
        file.addSeparator();
        file.add(item("Insert image…", "ctrl I", this::insertImage));
        file.addSeparator();
        file.add(item("Exit", null, this::dispose));
        bar.add(file);

        JMenu edit = new JMenu("Edit");
        edit.add(undo);
        edit.add(redo);
        edit.addSeparator();
        edit.add(duplicate);
        edit.add(delete);
        edit.add(group);
        edit.add(ungroup);
        edit.addSeparator();
        edit.add(item("Select all", "ctrl A", () -> selection.set(drawing.items().stream().map(Shape::id).toList())));
        bar.add(edit);

        JMenu toolsMenu = new JMenu("Tools");
        ButtonGroup radio = new ButtonGroup();
        for (Tool tool : tools.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(tool.name() + "    " + TOOL_KEYS.get(tool.name()));
            item.addActionListener(e -> use(tool));
            radio.add(item);
            toolsMenu.add(item);
            toolItems.put(tool, item);
        }
        bar.add(toolsMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.add(item("Zoom in", "ctrl EQUALS", () -> canvas.zoomBy(1.25)));
        viewMenu.add(item("Zoom out", "ctrl MINUS", () -> canvas.zoomBy(0.8)));
        viewMenu.add(fit);
        viewMenu.addSeparator();
        JCheckBoxMenuItem snap = new JCheckBoxMenuItem("Snap to grid", canvas.isSnapping());
        snap.addActionListener(e -> canvas.setSnapping(snap.isSelected()));
        viewMenu.add(snap);
        viewMenu.add(item("Light or dark theme", null, Theme::toggle));
        bar.add(viewMenu);

        JMenu help = new JMenu("Help");
        help.add(item("Commands", "F1", () -> run("help")));
        help.add(item("Example drawing", "ctrl shift E", this::openExample));
        help.add(item("About MiniCAD", null, this::about));
        bar.add(help);
        return bar;
    }

    private JPanel statusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        where.setPreferredSize(new Dimension(150, 18));
        bar.add(where, BorderLayout.WEST);
        bar.add(what, BorderLayout.CENTER);
        bar.add(view, BorderLayout.EAST);
        return bar;
    }

    private static JLabel status() {
        JLabel label = new JLabel(" ");
        label.putClientProperty("FlatLaf.styleClass", "small");
        return label;
    }

    private void bindCanvasKeys() {
        Map<String, Runnable> keys = new LinkedHashMap<>();
        TOOL_KEYS.forEach((name, key) -> keys.put(key, () -> use(tools.get(name))));
        keys.put("DELETE", this::delete);
        keys.put("BACK_SPACE", this::delete);
        keys.put("ESCAPE", selection::clear);
        double step = Canvas.GRID;
        keys.put("LEFT", () -> nudge(-step, 0));
        keys.put("RIGHT", () -> nudge(step, 0));
        keys.put("UP", () -> nudge(0, -step));
        keys.put("DOWN", () -> nudge(0, step));
        keys.put("shift LEFT", () -> nudge(-1, 0));
        keys.put("shift RIGHT", () -> nudge(1, 0));
        keys.put("shift UP", () -> nudge(0, -1));
        keys.put("shift DOWN", () -> nudge(0, 1));
        keys.forEach((key, action) -> {
            canvas.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), key);
            canvas.getActionMap().put(key, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            });
        });
    }

    // ---- actions ---------------------------------------------------------------------------

    private void use(Tool tool) {
        canvas.setTool(tool);
        toolButtons.get(tool).setSelected(true);
        toolItems.get(tool).setSelected(true);
        refreshView();
    }

    private List<Shape> selected() {
        return selection.shapes(drawing);
    }

    private void duplicate() {
        List<Shape> shapes = selected();
        List<String> copies = new ArrayList<>();
        for (Shape shape : shapes) {
            if (run("copy " + shape.id())) {
                copies.add(drawing.items().getLast().id());
            }
        }
        selection.set(copies);
    }

    private void delete() {
        if (!selection.isEmpty()) {
            run("del " + ScriptWriter.ids(selected()));
        }
    }

    private void group() {
        List<Shape> shapes = selected();
        if (shapes.size() > 1 && run("grp " + ScriptWriter.ids(shapes))) {
            drawing.find(shapes.getFirst().id()).flatMap(drawing::parentOf).ifPresent(g -> selection.set(g.id()));
        }
    }

    private void ungroup() {
        if (selected().size() == 1 && selected().getFirst() instanceof Group g) {
            List<String> members = g.members().stream().map(Shape::id).toList();
            if (run("ungrp " + g.id())) {
                selection.set(members);
            }
        }
    }

    private void nudge(double dx, double dy) {
        if (!selection.isEmpty()) {
            run("mvoff " + ScriptWriter.ids(selected()) + " " + new Point(dx, dy));
        }
    }

    private JButton paletteButton(String tip, Icon icon, String which) {
        JButton button = new JButton(icon);
        button.setToolTipText(tip + " of the selection");
        button.addActionListener(e -> {
            if (selection.isEmpty()) {
                console.note("Select some shapes first: click them, or pick them in the list on the left.");
                return;
            }
            JPopupMenu menu = new JPopupMenu();
            JPanel grid = new JPanel(new GridLayout(2, 6, 4, 4));
            grid.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            for (Rgb colour : PALETTE) {
                JButton swatch = new JButton(Icons.swatch(Java2DRenderer.colour(colour)));
                swatch.setToolTipText(colour.hex());
                swatch.putClientProperty("JButton.buttonType", "toolBarButton");
                swatch.addActionListener(a -> {
                    menu.setVisible(false);
                    recolour(which, colour.hex());
                });
                grid.add(swatch);
            }
            menu.add(grid);
            menu.addSeparator();
            menu.add(item("None", null, () -> recolour(which, "none")));
            menu.add(item("Custom…", null, () -> {
                Color chosen = JColorChooser.showDialog(this, tip, Theme.ACCENT);
                if (chosen != null) {
                    recolour(which, new Rgb(chosen.getRGB() & 0xFFFFFF).hex());
                }
            }));
            menu.show(button, 0, button.getHeight());
        });
        paletteButtons.add(button);
        return button;
    }

    private void recolour(String which, String colour) {
        run("color " + ScriptWriter.ids(selected()) + " " + which + " " + colour);
    }

    private void open() {
        chooseFile("Open a drawing", "MiniCAD drawings", "mcad", false)
                .ifPresent(file -> {
                    if (run("load " + ScriptWriter.quote(file.getPath()))) {
                        canvasLater(Canvas::fit);
                    }
                });
    }

    private void save() {
        chooseFile("Save the drawing", "MiniCAD drawings", "mcad", true)
                .ifPresent(file -> run("save " + ScriptWriter.quote(file.getPath())));
    }

    private void export() {
        chooseFile("Export as SVG", "SVG images", "svg", true)
                .ifPresent(file -> run("export " + ScriptWriter.quote(file.getPath())));
    }

    private void insertImage() {
        JFileChooser chooser = new JFileChooser(folder);
        chooser.setDialogTitle("Insert an image");
        chooser.setFileFilter(new FileNameExtensionFilter("Images (PNG, JPEG, GIF, BMP)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            folder = chooser.getCurrentDirectory();
            Point centre = canvas.snap(canvas.toDrawing(canvas.getWidth() / 2.0, canvas.getHeight() / 2.0));
            if (run("new image " + ScriptWriter.quote(chooser.getSelectedFile().getPath()) + " " + centre)) {
                selection.set(drawing.items().getLast().id());
            }
        }
    }

    private Optional<File> chooseFile(String title, String kind, String extension, boolean saving) {
        JFileChooser chooser = new JFileChooser(folder);
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter(kind + " (*." + extension + ")", extension));
        int answer = saving ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        if (answer != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        folder = chooser.getCurrentDirectory();
        File file = chooser.getSelectedFile();
        if (saving && !file.getName().toLowerCase(Locale.ROOT).endsWith("." + extension)) {
            file = new File(file.getPath() + "." + extension);
        }
        return Optional.of(file);
    }

    /** Opens the garden of the report's second scenario, shipped inside the jar. */
    void openExample() {
        List<String> lines;
        try (InputStream in = Objects.requireNonNull(getClass().getResourceAsStream("/minicad/examples/garden.mcad"))) {
            lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        console.note("Opening the example: garden.mcad, " + lines.size() + " lines of commands.");
        if (editor.run(workspace -> editor.replace("garden.mcad", lines), "open the example")) {
            selection.clear();
            canvasLater(Canvas::fit);
        }
        refresh();
    }

    private void about() {
        JOptionPane.showMessageDialog(this, """
                        <html><b>MiniCAD</b> 2.0<br><br>
                        A small 2D CAD driven by a command language,<br>
                        built to put the GoF design patterns to work.<br><br>
                        Davide Cinelli<br>
                        Ingegneria del Software, University of Calabria, A.Y. 2023/2024</html>""",
                "About MiniCAD", JOptionPane.PLAIN_MESSAGE, new javax.swing.ImageIcon(Icons.logo(64)));
    }

    // ---- state -----------------------------------------------------------------------------

    private void refresh() {
        undo.setEnabled(editor.history().nextUndo().isPresent());
        redo.setEnabled(editor.history().nextRedo().isPresent());
        undo.putValue(Action.SHORT_DESCRIPTION, editor.history().nextUndo().map(l -> "Undo: " + l).orElse("Undo"));
        redo.putValue(Action.SHORT_DESCRIPTION, editor.history().nextRedo().map(l -> "Redo: " + l).orElse("Redo"));
        List<Shape> shapes = selected();
        duplicate.setEnabled(!shapes.isEmpty());
        delete.setEnabled(!shapes.isEmpty());
        group.setEnabled(shapes.size() > 1);
        ungroup.setEnabled(shapes.size() == 1 && shapes.getFirst() instanceof Group);
        paletteButtons.forEach(b -> b.setEnabled(!shapes.isEmpty()));
        fit.setEnabled(!drawing.isEmpty());
        if (shapes.isEmpty()) {
            long count = drawing.shapes().filter(s -> !(s instanceof Group)).count();
            what.setText(count == 0 ? "Empty drawing" : count + (count == 1 ? " shape" : " shapes"));
        } else if (shapes.size() == 1) {
            Shape s = shapes.getFirst();
            what.setText(s.id() + "  " + s.kind() + "   ·   area " + Numbers.brief(s.area())
                    + "   ·   perimeter " + Numbers.brief(s.perimeter()));
        } else {
            what.setText(shapes.size() + " selected   ·   total area "
                    + Numbers.brief(shapes.stream().mapToDouble(Shape::area).sum()));
        }
        refreshView();
    }

    private void refreshView() {
        Point p = canvas.cursor();
        where.setText(p == null ? " " : "x " + Numbers.brief(p.x()) + "   y " + Numbers.brief(p.y()));
        view.setText(canvas.tool().name() + "   ·   " + Math.round(canvas.zoom() * 100) + "%   ·   snap "
                + (canvas.isSnapping() ? "on" : "off"));
    }

    private void canvasLater(Consumer<Canvas> step) {
        SwingUtilities.invokeLater(() -> step.accept(canvas));
    }

    private List<String> idsInDrawing() {
        return drawing.shapes().map(Shape::id).toList();
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static Action action(String name, Icon icon, String key, Runnable body) {
        AbstractAction action = new AbstractAction(name, icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                body.run();
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, name);
        if (key != null) {
            action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(key));
        }
        return action;
    }

    private static JMenuItem item(String name, String key, Runnable body) {
        JMenuItem item = new JMenuItem(name);
        if (key != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(key));
        }
        item.addActionListener(e -> body.run());
        return item;
    }

    private static JButton button(Action action) {
        JButton button = new JButton(action);
        button.setHideActionText(true);
        button.setFocusable(false);
        return button;
    }
}
