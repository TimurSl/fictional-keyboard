package com.enchanting.keyboard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Voices of the Void "Ariral" translator.
 *
 * The wiki only describes phonetic rules (syllables are a vowel plus a consonant, a sharp R,
 * no silent letters, almost every word ends in "-a", Apple -> Appule), so this is a rule-based
 * approximation of how an Ariral would pronounce English or Russian text.
 */
public final class Ariral {
    private Ariral() {}

    private static final String VOWELS = "aeiou";
    private static final Set<String> DIGRAPHS = new HashSet<>(Arrays.asList(
        "th", "sh", "ch", "ph", "ck", "ng", "zh", "kh", "ts", "wh"));

    // Consonant pairs that can plausibly be an inserted-"u" cluster when decoding.
    private static final Set<String> CLUSTERS = new HashSet<>(Arrays.asList(
        "bl", "br", "cl", "cr", "dr", "fl", "fr", "gl", "gr", "pl", "pr", "sc", "sk", "sl", "sm",
        "sn", "sp", "st", "sw", "tr", "tw", "nd", "nt", "ld", "lt", "rd", "rt", "rn", "rm", "rl",
        "mp", "nk", "ft", "ps", "ks", "ls", "ns", "rs", "ts", "ds", "ms", "bs", "gs", "lk", "lm",
        "lp", "rk", "rp", "rb", "rg", "lb", "lf", "mb", "nc", "nf", "ngl", "xt"));

    /** Translates running text word by word. */
    public static String translate(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (String t : Ozkavosh.tokenize(text)) {
            if (!t.isEmpty() && Translit.isWordChar(t.charAt(0))) out.append(translateWord(t));
            else out.append(t);
        }
        return out.toString();
    }

    public static String translateWord(String word) {
        if (word == null || word.isEmpty()) return "";
        boolean capital = Character.isUpperCase(word.charAt(0));
        String w = Translit.toLatin(word).replace("'", "").replace("’", "");
        if (w.isEmpty()) return "";

        String result;
        if (isLaugh(w)) {
            result = "ack";
        } else {
            result = convert(w);
        }
        if (capital && !result.isEmpty()) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }
        return result;
    }

    private static boolean isLaugh(String w) {
        return w.equals("lol") || w.matches("(ha)+h?") || w.matches("(kha)+h?");
    }

    private static boolean isVowelAt(String w, int i) {
        char c = w.charAt(i);
        if (VOWELS.indexOf(c) >= 0) return true;
        if (c == 'y') {
            // y is a vowel unless it starts the word or follows a vowel
            return i > 0 && VOWELS.indexOf(w.charAt(i - 1)) < 0;
        }
        return false;
    }

    private static boolean isConsonantAt(String w, int i) {
        return Character.isLetter(w.charAt(i)) && !isVowelAt(w, i);
    }

    private static String convert(String w) {
        // Rule 2: silent letters
        if (w.startsWith("kn")) w = w.substring(1);
        else if (w.startsWith("wr")) w = w.substring(1);
        w = dropGh(w);

        // Final "e": cluster + e keeps the e (-ule), otherwise it is silent and dropped
        boolean keepE = false;
        int n = w.length();
        if (n >= 3 && w.charAt(n - 1) == 'e' && isConsonantAt(w, n - 2)) {
            if (isConsonantAt(w, n - 3)) keepE = true;
            else if (hasVowelBefore(w, n - 2)) w = w.substring(0, n - 1);
        }

        // Rule 3: break consonant clusters with "u" (not doubled letters, not digraphs)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w.length(); i++) {
            sb.append(w.charAt(i));
            if (i + 1 < w.length() && isConsonantAt(w, i) && isConsonantAt(w, i + 1)
                    && w.charAt(i) != w.charAt(i + 1)
                    && !DIGRAPHS.contains(w.substring(i, i + 2))
) {
                sb.append('u');
            }
        }
        String r = sb.toString();

        // Rule 4: single r -> rr
        StringBuilder rr = new StringBuilder();
        for (int i = 0; i < r.length(); i++) {
            char c = r.charAt(i);
            boolean single = c == 'r'
                && !(i > 0 && r.charAt(i - 1) == 'r')
                && !(i + 1 < r.length() && r.charAt(i + 1) == 'r');
            rr.append(c);
            if (single) rr.append('r');
        }
        r = rr.toString();

        // Rule 5 is handled by keepE; Rule 6: end with "a" unless the word ends in a vowel
        if (!keepE) {
            char last = r.charAt(r.length() - 1);
            if (Character.isLetter(last) && !isVowelAt(r, r.length() - 1)) r += "a";
        }
        return r;
    }

    private static boolean hasVowelBefore(String w, int end) {
        for (int i = 0; i < end; i++) if (isVowelAt(w, i)) return true;
        return false;
    }

    private static String dropGh(String w) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w.length(); i++) {
            if (i > 0 && w.charAt(i) == 'g' && i + 1 < w.length() && w.charAt(i + 1) == 'h') {
                i++;
                continue;
            }
            sb.append(w.charAt(i));
        }
        return sb.toString();
    }

    // ---- Reverse (heuristic) ----

    /** Best-effort reverse translation. Lossy: original "rr", "-a" endings and inner "u"s are ambiguous. */
    public static String decode(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (String t : Ozkavosh.tokenize(text)) {
            if (!t.isEmpty() && Translit.isWordChar(t.charAt(0))) out.append(decodeWord(t));
            else out.append(t);
        }
        return out.toString();
    }

    public static String decodeWord(String word) {
        String w = word.toLowerCase();
        if (w.equals("ack")) return "lol";
        // strip trailing "a" after a consonant
        if (w.length() > 2 && w.endsWith("a") && isConsonantAt(w, w.length() - 2)) {
            w = w.substring(0, w.length() - 1);
        }
        w = w.replace("rr", "r");
        w = removeInsertedU(w);
        return w;
    }

    private static String removeInsertedU(String w) {
        int vowels = 0;
        for (int i = 0; i < w.length(); i++) if (isVowelAt(w, i)) vowels++;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (c == 'u' && vowels > 1 && i > 0 && i + 1 < w.length()
                    && isConsonantAt(w, i - 1) && isConsonantAt(w, i + 1)
                    && CLUSTERS.contains(w.substring(i - 1, i) + w.charAt(i + 1))) {
                vowels--;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** Rough 0..1 likelihood that Latin text is Ariral (many words end in "a"/"e" with "rr"/"u" clusters). */
    static double score(String text) {
        int words = 0, aEnd = 0, marks = 0;
        for (String t : Ozkavosh.tokenize(text)) {
            if (t.isEmpty() || !Translit.isWordChar(t.charAt(0))) continue;
            for (char c : t.toCharArray()) if (c > 127) return 0;
            String w = t.toLowerCase();
            words++;
            if (w.endsWith("a") || w.endsWith("ule")) aEnd++;
            if (w.contains("rr")) marks++;
        }
        if (words < 2) return 0;
        return marks > 0 ? (double) aEnd / words : (double) aEnd / words * 0.5;
    }
}
