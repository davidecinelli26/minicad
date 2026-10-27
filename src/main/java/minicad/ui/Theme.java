package minicad.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Light and dark look, and the colours of the canvas. The canvas is paper: white in both themes. */
final class Theme {

    static final Color ACCENT = new Color(0x2a9d8f);
    static final Color PAPER = new Color(0xffffff);
    static final Color GRID_MINOR = new Color(0xeef1f5);
    static final Color GRID_MAJOR = new Color(0xdce2e9);
    static final Color AXIS = new Color(0xc3ccd6);
    static final Color HINT = new Color(0x94a3b8);
    static final Color ERROR = new Color(0xe05263);

    private static boolean dark;
    private static String monoFamily;

    private Theme() {
    }

    static void install(boolean dark) {
        Theme.dark = dark;
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#2a9d8f"));
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("SplitPaneDivider.style", "plain");
        if (dark) {
            FlatMacDarkLaf.setup();
        } else {
            FlatMacLightLaf.setup();
        }
    }

    static void toggle() {
        install(!dark);
        FlatLaf.updateUI();
    }

    static boolean isDark() {
        return dark;
    }

    static Color muted() {
        return UIManager.getColor("Label.disabledForeground");
    }

    static Color foreground() {
        return UIManager.getColor("Label.foreground");
    }

    /** A monospaced font that looks good on the system: Cascadia, JetBrains Mono, Consolas, Menlo... */
    static Font mono(float size) {
        if (monoFamily == null) {
            Set<String> installed = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            monoFamily = List.of("Cascadia Mono", "JetBrains Mono", "Consolas", "Menlo", "SF Mono", "DejaVu Sans Mono")
                    .stream().filter(installed::contains).findFirst().orElse(Font.MONOSPACED);
        }
        return new Font(monoFamily, Font.PLAIN, 1).deriveFont(size);
    }
}
