package minicad.lang;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** The commands of the language, with their syntax: one source for the parser, help and completion. */
public enum Verb {

    NEW("create a shape",
            "new circle (r) (x,y)",
            "new rectangle (x,y) (w,h)",
            "new image \"file\" (x,y)",
            "  each can end with  fill #rrggbb  and  stroke #rrggbb  (or none)"),
    COPY("duplicate a shape or a whole group", "copy ID [(dx,dy)]"),
    DEL("delete shapes; a group goes with its members", "del ID..."),
    MV("move the centre of a shape to a point", "mv ID (x,y)"),
    MVOFF("move shapes by an offset", "mvoff ID... (dx,dy)"),
    SCALE("resize a shape about its centre", "scale ID factor"),
    GRP("group shapes, groups included", "grp ID ID..."),
    UNGRP("dissolve a group", "ungrp ID"),
    COLOR("change fill and outline", "color ID... #rrggbb", "color ID... [fill #rrggbb|none] [stroke #rrggbb|none]"),
    LS("describe shapes", "ls ID... | ls circle|rectangle|image|groups | ls all"),
    AREA("area of shapes", "area ID... | area circle|rectangle|image|groups | area all"),
    PERIMETER("perimeter of shapes", "perimeter ID... | perimeter TYPE | perimeter all"),
    UNDO("undo the last edit", "undo"),
    REDO("redo what undo undid", "redo"),
    SAVE("save the drawing as a script", "save \"file.mcad\""),
    LOAD("open a saved drawing", "load \"file.mcad\""),
    EXPORT("export the drawing as an SVG image", "export \"file.svg\""),
    CLEAR("delete everything", "clear"),
    HELP("list the commands, or show how one is written", "help [command]");

    private final String summary;
    private final List<String> usage;

    Verb(String summary, String... usage) {
        this.summary = summary;
        this.usage = List.of(usage);
    }

    /** The word that starts the command. */
    public String keyword() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String summary() {
        return summary;
    }

    public List<String> usage() {
        return usage;
    }

    /** The usage on one line, for error messages. */
    public String hint() {
        return String.join("  |  ", usage.stream().filter(line -> !line.startsWith(" ")).toList());
    }

    public static Optional<Verb> of(String word) {
        return Arrays.stream(values()).filter(v -> v.keyword().equalsIgnoreCase(word)).findFirst();
    }

    /** Words people try first, mapped to the command they mean. */
    private static final Map<String, Verb> SYNONYMS = Map.ofEntries(
            Map.entry("create", NEW), Map.entry("add", NEW), Map.entry("delete", DEL), Map.entry("remove", DEL),
            Map.entry("rm", DEL), Map.entry("move", MV), Map.entry("group", GRP), Map.entry("ungroup", UNGRP),
            Map.entry("colour", COLOR), Map.entry("list", LS), Map.entry("open", LOAD), Map.entry("duplicate", COPY),
            Map.entry("resize", SCALE), Map.entry("zoom", SCALE));

    /** The command a mistyped word most likely meant, if one is close enough. */
    public static Optional<Verb> closest(String word) {
        String typed = word.toLowerCase(Locale.ROOT);
        if (SYNONYMS.containsKey(typed)) {
            return Optional.of(SYNONYMS.get(typed));
        }
        int tolerance = typed.length() <= 3 ? 1 : 2;
        return Arrays.stream(values())
                .filter(v -> distance(typed, v.keyword()) <= tolerance)
                .min(Comparator.comparingInt(v -> distance(typed, v.keyword())));
    }

    /** Levenshtein distance. */
    static int distance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j], current[j - 1]) + 1);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
