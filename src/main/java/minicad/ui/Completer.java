package minicad.ui;

import minicad.lang.Verb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Tab completion for the console: command names, then ids, kinds and keywords that fit there. */
final class Completer {

    /** The line after completing, and the choices left when the word is still ambiguous. */
    record Result(String line, List<String> choices) {
    }

    private Completer() {
    }

    static Result complete(String line, Collection<String> ids) {
        int start = wordStart(line);
        String prefix = line.substring(start).toLowerCase(Locale.ROOT);
        String before = line.substring(0, start).trim();
        List<String> choices = candidates(before, ids).stream()
                .filter(word -> word.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .toList();
        if (choices.isEmpty()) {
            return new Result(line, List.of());
        }
        if (choices.size() == 1) {
            return new Result(line.substring(0, start) + choices.getFirst() + " ", List.of());
        }
        return new Result(line.substring(0, start) + commonPrefix(choices), choices);
    }

    private static int wordStart(String line) {
        int i = line.length();
        while (i > 0 && !Character.isWhitespace(line.charAt(i - 1)) && "(,".indexOf(line.charAt(i - 1)) < 0) {
            i--;
        }
        return i;
    }

    private static List<String> candidates(String before, Collection<String> ids) {
        List<String> verbs = Arrays.stream(Verb.values()).map(Verb::keyword).toList();
        if (before.isEmpty()) {
            return verbs;
        }
        String[] words = before.split("\\s+");
        String verb = words[0].toLowerCase(Locale.ROOT);
        List<String> sorted = new ArrayList<>(ids);
        sorted.sort(Completer::byNumber);
        return switch (verb) {
            case "new" -> words.length == 1 ? List.of("circle", "rectangle", "image") : List.of("fill", "stroke", "none");
            case "ls", "area", "perimeter" -> Stream.concat(Stream.of("all", "circle", "rectangle", "image", "groups"),
                    sorted.stream()).toList();
            case "color" -> Stream.concat(sorted.stream(), Stream.of("fill", "stroke", "none")).toList();
            case "help" -> verbs;
            case "ungrp" -> sorted.stream().filter(id -> id.startsWith("grp")).toList();
            case "undo", "redo", "clear", "save", "load", "export" -> List.of();
            default -> sorted;
        };
    }

    /** obj2 before obj10. */
    private static int byNumber(String a, String b) {
        String pa = a.replaceAll("\\d+$", ""), pb = b.replaceAll("\\d+$", "");
        if (!pa.equals(pb)) {
            return pa.compareTo(pb);
        }
        return Long.compare(number(a), number(b));
    }

    private static long number(String id) {
        String digits = id.replaceAll("^\\D+", "");
        return digits.isEmpty() ? -1 : Long.parseLong(digits);
    }

    private static String commonPrefix(List<String> words) {
        String prefix = words.getFirst();
        for (String word : words) {
            while (!word.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
        }
        return prefix;
    }
}
