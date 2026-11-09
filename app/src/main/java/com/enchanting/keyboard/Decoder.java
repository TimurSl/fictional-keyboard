package com.enchanting.keyboard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Turns glyph text (SGA, runes, gothic, zalgo) and game-language text back into readable text. */
public final class Decoder {
    private Decoder() {}

    public enum Script {
        AUTO("Auto"), SGA("SGA"), RUNES("Runes"), GOTHIC("Gothic"), ZALGO("Zalgo"),
        OZKAVOSH("Ozkavosh"), ARIRAL("Ariral"), NONE("Plain text");

        public final String label;
        Script(String label) { this.label = label; }
    }

    /** Which alphabet to read ambiguous Latin/Cyrillic glyphs as. */
    public enum Target {
        AUTO("Auto"), EN("EN"), RU("RU");

        public final String label;
        Target(String label) { this.label = label; }

        public static Target fromName(String name) {
            for (Target t : values()) if (t.name().equals(name)) return t;
            return AUTO;
        }
    }

    public static final class Result {
        public final Script script;
        public final Target target;
        public final String text;
        Result(Script script, Target target, String text) {
            this.script = script;
            this.target = target;
            this.text = text;
        }
    }

    /** Glyph tables for one script: glyph string -> letter, for Latin and Cyrillic readings. */
    private static final class Table {
        final Map<String, String> en = new HashMap<>();
        final Map<String, String> ru = new HashMap<>();
        int maxLen = 1;
        final Set<Integer> glyphChars = new HashSet<>();
    }

    private static final String LATIN = "abcdefghijklmnopqrstuvwxyz";
    private static final String CYRILLIC = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";

    private static final Table SGA = build(Translators.sgaMap(), "", "");
    private static final Table RUNES = build(Translators.futharkMap(), "kiw", "з");
    private static final Table GOTHIC = build(Translators.gothicMap(), "", "");

    private static Table build(Map<Character, String> map, String latinPref, String cyrPref) {
        Table t = new Table();
        for (char c : (latinPref + LATIN).toCharArray()) {
            String g = map.get(c);
            if (g != null && !g.isEmpty() && !t.en.containsKey(g)) t.en.put(g, String.valueOf(c));
        }
        for (char c : (cyrPref + CYRILLIC).toCharArray()) {
            String g = map.get(c);
            if (g != null && !g.isEmpty() && !t.ru.containsKey(g)) t.ru.put(g, String.valueOf(c));
        }
        for (Map.Entry<Character, String> e : map.entrySet()) {
            String g = e.getValue();
            t.maxLen = Math.max(t.maxLen, g.length());
            for (int i = 0; i < g.length(); ) {
                int cp = g.codePointAt(i);
                i += Character.charCount(cp);
                if (cp > 127 && cp != ' ' && !isCombining(cp)) t.glyphChars.add(cp);
            }
        }
        return t;
    }

    private static boolean isCombining(int cp) {
        return cp >= 0x0300 && cp <= 0x036F;
    }

    // Sequences that only the Cyrillic alphabet produces (in English they would be ch/sh/ya/zh).
    private static final String[] SGA_RU_HINTS = {"||ᔑ", "ᓭ⍑", "ᓵ⍑", "⨅⍑"};
    private static final String[] RUNES_RU_HINTS = {"ᛃᚨ", "ᛋᚺ", "ᚲᚺ"};
    // Glyphs only Latin letters can produce in SGA (Cyrillic has no q, w or x).
    private static final String SGA_LATIN_ONLY = "ᑑ∴⨉";

    // ---- Detection ----

    public static Script detect(String text) {
        if (text == null || text.trim().isEmpty()) return Script.NONE;

        int total = 0, sga = 0, runes = 0, gothic = 0, combining = 0, letters = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isWhitespace(cp)) continue;
            total++;
            if (SGA.glyphChars.contains(cp)) sga++;
            if (RUNES.glyphChars.contains(cp)) runes++;
            if (GOTHIC.glyphChars.contains(cp)) gothic++;
            if (isCombining(cp)) combining++;
            else if (Character.isLetterOrDigit(cp)) letters++;
        }
        if (total == 0) return Script.NONE;

        double sgaR = (double) sga / total;
        double runR = (double) runes / total;
        double gotR = (double) gothic / total;
        double best = Math.max(sgaR, Math.max(runR, gotR));
        if (best >= 0.3) {
            if (best == sgaR) return Script.SGA;
            if (best == runR) return Script.RUNES;
            return Script.GOTHIC;
        }
        if (combining >= 2 && combining >= letters) return Script.ZALGO;
        if (Ozkavosh.score(text) >= 0.6) return Script.OZKAVOSH;
        if (Ariral.score(text) >= 0.7) return Script.ARIRAL;
        return Script.NONE;
    }

    // ---- Decoding ----

    public static String decode(String text, Script script, Target target) {
        return decodeDetailed(text, script, target).text;
    }

    public static Result decodeDetailed(String text, Script script, Target target) {
        if (text == null) text = "";
        if (script == null || script == Script.AUTO) script = detect(text);
        if (target == null) target = Target.AUTO;

        switch (script) {
            case SGA: {
                Target t = resolveTarget(text, SGA, target);
                return new Result(script, t, decodeGlyphs(text, SGA, t, true));
            }
            case RUNES: {
                Target t = resolveTarget(text, RUNES, target);
                return new Result(script, t, decodeGlyphs(text, RUNES, t, false));
            }
            case GOTHIC: {
                // Gothic covers Latin only; Cyrillic typed in Gothic mode falls back to SGA glyphs.
                String latin = decodeGlyphs(text, GOTHIC, Target.EN, false);
                Target t = resolveTarget(latin, SGA, target);
                return new Result(script, t, decodeGlyphs(latin, SGA, t, true));
            }
            case ZALGO:
                return new Result(script, target, stripCombining(text));
            case OZKAVOSH: {
                Target t = target == Target.RU ? Target.RU : Target.EN;
                return new Result(script, t, Ozkavosh.decode(text, t == Target.RU));
            }
            case ARIRAL:
                return new Result(script, Target.EN, Ariral.decode(text));
            default:
                return new Result(Script.NONE, target, text);
        }
    }

    public static String stripCombining(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!isCombining(c)) sb.append(c);
        }
        return sb.toString();
    }

    private static Target resolveTarget(String text, Table table, Target requested) {
        if (requested != Target.AUTO) return requested;
        boolean isSga = table == SGA;
        if (isSga) {
            for (int i = 0; i < SGA_LATIN_ONLY.length(); i++) {
                if (text.indexOf(SGA_LATIN_ONLY.charAt(i)) >= 0) return Target.EN;
            }
        }
        String[] hints = isSga ? SGA_RU_HINTS : RUNES_RU_HINTS;
        for (String h : hints) if (text.contains(h)) return Target.RU;
        return Target.EN;
    }

    private static final String RU_VOWELS = "аеёиоуыэюя";

    private static String decodeGlyphs(String text, Table table, Target target, boolean sgaYHeuristic) {
        boolean ru = target == Target.RU;
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            String match = null;
            String letter = null;
            int max = Math.min(table.maxLen, text.length() - i);
            for (int len = max; len >= 1 && match == null; len--) {
                String cand = text.substring(i, i + len);
                String hit = null;
                if (ru) hit = table.ru.get(cand);
                if (hit == null) hit = table.en.get(cand);
                if (hit != null) {
                    match = cand;
                    letter = hit;
                }
            }
            if (match == null) {
                out.append(text.charAt(i));
                i++;
                continue;
            }
            // In SGA, Russian "й" and "ы" share one glyph: й after a vowel (or at word start), ы after a consonant.
            if (ru && sgaYHeuristic && match.equals("||")) {
                char prev = out.length() > 0 ? out.charAt(out.length() - 1) : ' ';
                boolean afterConsonant = Character.isLetter(prev) && RU_VOWELS.indexOf(prev) < 0
                        && "ъь".indexOf(prev) < 0;
                letter = afterConsonant ? "ы" : "й";
            }
            out.append(letter);
            i += match.length();
        }
        return out.toString();
    }
}
