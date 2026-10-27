package minicad.ui;

import minicad.model.Drawing;
import minicad.model.DrawingListener;
import minicad.model.Figure;
import minicad.model.Group;
import minicad.model.Shape;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The Composite made visible: groups and their members as a tree, top of the drawing first.
 * Another observer of the drawing, and of the selection, which it keeps in step with the canvas.
 */
final class ObjectTree extends JPanel implements DrawingListener {

    private final Drawing drawing;
    private final Selection selection;
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Drawing");
    private final DefaultTreeModel model = new DefaultTreeModel(root);
    private final JTree tree = new JTree(model);
    private final JLabel title = new JLabel();
    private boolean syncing;

    ObjectTree(Drawing drawing, Selection selection) {
        super(new BorderLayout());
        this.drawing = drawing;
        this.selection = selection;
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(24);
        tree.setCellRenderer(new Renderer());
        tree.putClientProperty("JTree.lineStyle", "None");
        tree.addTreeSelectionListener(e -> {
            if (!syncing) {
                List<String> ids = new ArrayList<>();
                TreePath[] paths = tree.getSelectionPaths();
                for (TreePath path : paths == null ? new TreePath[0] : paths) {
                    if (((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject() instanceof Shape shape) {
                        ids.add(shape.id());
                    }
                }
                syncing = true;
                selection.set(ids);
                syncing = false;
            }
        });
        selection.addListener(this::showSelection);
        drawing.addListener(this);

        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setBorder(BorderFactory.createEmptyBorder(8, 10, 6, 10));
        add(title, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
        rebuild();
    }

    @Override
    public void drawingChanged(Change change) {
        if (change == Change.STRUCTURE) {
            rebuild();
        } else {
            tree.repaint();
        }
    }

    private void rebuild() {
        // Reloading clears the tree's own selection: that must not clear the shared one.
        syncing = true;
        root.removeAllChildren();
        List<Shape> items = new ArrayList<>(drawing.items());
        Collections.reverse(items);
        items.forEach(shape -> root.add(node(shape)));
        model.reload();
        for (int row = 0; row < tree.getRowCount(); row++) {
            tree.expandRow(row);
        }
        syncing = false;
        long count = drawing.shapes().filter(shape -> !(shape instanceof Group)).count();
        title.setText("Shapes  " + (count == 0 ? "" : "(" + count + ")"));
        showSelection();
    }

    private static DefaultMutableTreeNode node(Shape shape) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(shape);
        if (shape instanceof Group group) {
            List<Shape> members = new ArrayList<>(group.members());
            Collections.reverse(members);
            members.forEach(member -> node.add(node(member)));
        }
        return node;
    }

    private void showSelection() {
        if (syncing) {
            return;
        }
        List<TreePath> paths = new ArrayList<>();
        Collections.list(root.depthFirstEnumeration()).forEach(node -> {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) node;
            if (n.getUserObject() instanceof Shape shape && selection.contains(shape.id())) {
                paths.add(new TreePath(n.getPath()));
            }
        });
        syncing = true;
        tree.setSelectionPaths(paths.toArray(TreePath[]::new));
        if (!paths.isEmpty()) {
            tree.scrollPathToVisible(paths.getFirst());
        }
        syncing = false;
    }

    /** {@code obj3  circle}, with a swatch of its colours. */
    private static final class Renderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (((DefaultMutableTreeNode) value).getUserObject() instanceof Shape shape) {
                setText("<html><b>" + shape.id() + "</b>&nbsp;&nbsp;" + shape.kind() + "</html>");
                setIcon(icon(shape));
            }
            return this;
        }

        private static Icon icon(Shape shape) {
            if (shape instanceof Figure figure) {
                var style = figure.style();
                return Icons.swatch(style.fill() != null ? Java2DRenderer.colour(style.fill())
                        : style.stroke() != null ? Java2DRenderer.colour(style.stroke()) : null);
            }
            return Objects.requireNonNull(Icons.group());
        }
    }
}
