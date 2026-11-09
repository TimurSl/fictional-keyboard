package com.enchanting.keyboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Translators {

    public enum Mode {
        SGA("ᓵ⍑ SGA", "SGA"),
        RUNES("ᚱ Runes", "Runes"),
        ZALGO("҂ Zalgo", "Zalgo"),
        GOTHIC("𝔊 Gothic", "Gothic"),
        NORMAL("ABC Norm", "Normal"),
        DEMON("👹 Ozkavosh", "Ozkavosh"),
        ARIRAL("👽 Ariral", "Ariral");

        /** Chip text, with a decorative prefix. */
        public final String title;
        /** Plain name, shown on the space bar. */
        public final String label;
        Mode(String title, String label) {
            this.title = title;
            this.label = label;
        }

        /** Modes that translate whole words (need the full word before output). */
        public boolean isWordLevel() {
            return this == DEMON || this == ARIRAL;
        }
    }

    private static final Map<Character, String> SGA_MAP = new HashMap<>();
    private static final Map<Character, String> FUTHARK_MAP = new HashMap<>();
    private static final Map<Character, String> GOTHIC_MAP = new HashMap<>();
    private static final Random RNG = new Random();

    private static final String[] ZALGO_UP = {
        "\u0300", "\u0301", "\u0302", "\u0303", "\u0304", "\u0305", "\u0306", "\u0307", "\u0308", "\u030A",
        "\u030B", "\u030C", "\u030D", "\u030E", "\u030F", "\u0310", "\u0311", "\u0312", "\u0313", "\u0314"
    };
    private static final String[] ZALGO_MID = {
        "\u0334", "\u0335", "\u0336", "\u0337", "\u0338", "\u0315", "\u031B"
    };
    private static final String[] ZALGO_DOWN = {
        "\u0316", "\u0317", "\u0318", "\u0319", "\u031C", "\u031D", "\u031E", "\u031F", "\u0320", "\u0321",
        "\u0322", "\u0323", "\u0324", "\u0325", "\u0326", "\u0327", "\u0328", "\u0329", "\u032A", "\u032B"
    };

    static {
        // Standard Galactic Alphabet (Minecraft Enchanting Table)
        SGA_MAP.put('a', "ᔑ");
        SGA_MAP.put('b', "ʖ");
        SGA_MAP.put('c', "ᓵ");
        SGA_MAP.put('d', "↸");
        SGA_MAP.put('e', "ᒷ");
        SGA_MAP.put('f', "⎓");
        SGA_MAP.put('g', "⊣");
        SGA_MAP.put('h', "⍑");
        SGA_MAP.put('i', "╎");
        SGA_MAP.put('j', "⋮");
        SGA_MAP.put('k', "ꖌ");
        SGA_MAP.put('l', "ꖎ");
        SGA_MAP.put('m', "ᒲ");
        SGA_MAP.put('n', "リ");
        SGA_MAP.put('o', "𝙹");
        SGA_MAP.put('p', "!¡");
        SGA_MAP.put('q', "ᑑ");
        SGA_MAP.put('r', "ꋪ");
        SGA_MAP.put('s', "ᓭ");
        SGA_MAP.put('t', "ℸ ̣");
        SGA_MAP.put('u', "⚍");
        SGA_MAP.put('v', "⍊");
        SGA_MAP.put('w', "∴");
        SGA_MAP.put('x', "⨉");
        SGA_MAP.put('y', "||");
        SGA_MAP.put('z', "⨅");

        // Cyrillic -> SGA mapping
        SGA_MAP.put('а', "ᔑ");
        SGA_MAP.put('б', "ʖ");
        SGA_MAP.put('в', "⍊");
        SGA_MAP.put('г', "⊣");
        SGA_MAP.put('д', "↸");
        SGA_MAP.put('е', "ᒷ");
        SGA_MAP.put('ё', "||𝙹");
        SGA_MAP.put('ж', "⨅⍑");
        SGA_MAP.put('з', "⨅");
        SGA_MAP.put('и', "╎");
        SGA_MAP.put('й', "||");
        SGA_MAP.put('к', "ꖌ");
        SGA_MAP.put('л', "ꖎ");
        SGA_MAP.put('м', "ᒲ");
        SGA_MAP.put('н', "リ");
        SGA_MAP.put('о', "𝙹");
        SGA_MAP.put('п', "!¡");
        SGA_MAP.put('р', "ꋪ");
        SGA_MAP.put('с', "ᓭ");
        SGA_MAP.put('т', "ℸ ̣");
        SGA_MAP.put('у', "⚍");
        SGA_MAP.put('ф', "⎓");
        SGA_MAP.put('х', "⍑");
        SGA_MAP.put('ц', "ᓵ");
        SGA_MAP.put('ч', "ᓵ⍑");
        SGA_MAP.put('ш', "ᓭ⍑");
        SGA_MAP.put('щ', "ᓭ⍑ᓵ");
        SGA_MAP.put('ъ', "");
        SGA_MAP.put('ы', "||");
        SGA_MAP.put('ь', "");
        SGA_MAP.put('э', "ᒷ");
        SGA_MAP.put('ю', "||⚍");
        SGA_MAP.put('я', "||ᔑ");

        // Elder Futhark Runes
        FUTHARK_MAP.put('a', "ᚨ");
        FUTHARK_MAP.put('b', "ᛒ");
        FUTHARK_MAP.put('c', "ᚲ");
        FUTHARK_MAP.put('d', "ᛞ");
        FUTHARK_MAP.put('e', "ᛖ");
        FUTHARK_MAP.put('f', "ᚠ");
        FUTHARK_MAP.put('g', "ᚷ");
        FUTHARK_MAP.put('h', "ᚺ");
        FUTHARK_MAP.put('i', "ᛁ");
        FUTHARK_MAP.put('j', "ᛃ");
        FUTHARK_MAP.put('k', "ᚲ");
        FUTHARK_MAP.put('l', "ᛚ");
        FUTHARK_MAP.put('m', "ᛗ");
        FUTHARK_MAP.put('n', "ᚾ");
        FUTHARK_MAP.put('o', "ᛟ");
        FUTHARK_MAP.put('p', "ᛈ");
        FUTHARK_MAP.put('q', "ᚲ");
        FUTHARK_MAP.put('r', "ᚱ");
        FUTHARK_MAP.put('s', "ᛋ");
        FUTHARK_MAP.put('t', "ᛏ");
        FUTHARK_MAP.put('u', "ᚢ");
        FUTHARK_MAP.put('v', "ᚹ");
        FUTHARK_MAP.put('w', "ᚹ");
        FUTHARK_MAP.put('x', "ᚲᛋ");
        FUTHARK_MAP.put('y', "ᛁ");
        FUTHARK_MAP.put('z', "ᛉ");

        // Cyrillic -> Runes
        FUTHARK_MAP.put('а', "ᚨ");
        FUTHARK_MAP.put('б', "ᛒ");
        FUTHARK_MAP.put('в', "ᚹ");
        FUTHARK_MAP.put('г', "ᚷ");
        FUTHARK_MAP.put('д', "ᛞ");
        FUTHARK_MAP.put('е', "ᛖ");
        FUTHARK_MAP.put('ё', "ᛃᛟ");
        FUTHARK_MAP.put('ж', "ᛉ");
        FUTHARK_MAP.put('з', "ᛉ");
        FUTHARK_MAP.put('и', "ᛁ");
        FUTHARK_MAP.put('й', "ᛃ");
        FUTHARK_MAP.put('к', "ᚲ");
        FUTHARK_MAP.put('л', "ᛚ");
        FUTHARK_MAP.put('м', "ᛗ");
        FUTHARK_MAP.put('н', "ᚾ");
        FUTHARK_MAP.put('о', "ᛟ");
        FUTHARK_MAP.put('п', "ᛈ");
        FUTHARK_MAP.put('р', "ᚱ");
        FUTHARK_MAP.put('с', "ᛋ");
        FUTHARK_MAP.put('т', "ᛏ");
        FUTHARK_MAP.put('у', "ᚢ");
        FUTHARK_MAP.put('ф', "ᚠ");
        FUTHARK_MAP.put('х', "ᚺ");
        FUTHARK_MAP.put('ц', "ᚲᛋ");
        FUTHARK_MAP.put('ч', "ᚲᚺ");
        FUTHARK_MAP.put('ш', "ᛋᚺ");
        FUTHARK_MAP.put('щ', "ᛋᚺ");
        FUTHARK_MAP.put('ъ', "");
        FUTHARK_MAP.put('ы', "ᛁ");
        FUTHARK_MAP.put('ь', "");
        FUTHARK_MAP.put('э', "ᛖ");
        FUTHARK_MAP.put('ю', "ᛃᚢ");
        FUTHARK_MAP.put('я', "ᛃᚨ");

        // Gothic
        String latin = "abcdefghijklmnopqrstuvwxyz";
        String[] gothic = {
            "𝔞", "𝔟", "𝔠", "𝔡", "𝔢", "𝔣", "𝔤", "𝔥", "𝔦", "𝔧", "𝔨", "𝔩", "𝔪",
            "𝔫", "𝔬", "𝔭", "𝔮", "𝔯", "𝔰", "𝔱", "𝔲", "𝔳", "𝔴", "𝔵", "𝔶", "𝔷"
        };
        for (int i = 0; i < latin.length(); i++) {
            GOTHIC_MAP.put(latin.charAt(i), gothic[i]);
        }
    }

    public static String translateKey(String text, Mode mode) {
        if (text == null || text.isEmpty()) return "";
        if (mode == Mode.NORMAL) return text;
        if (mode == Mode.DEMON) return Ozkavosh.translate(text);
        if (mode == Mode.ARIRAL) return Ariral.translate(text);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char lower = Character.toLowerCase(c);

            switch (mode) {
                case SGA:
                    if (SGA_MAP.containsKey(lower)) {
                        sb.append(SGA_MAP.get(lower));
                    } else {
                        sb.append(c);
                    }
                    break;
                case RUNES:
                    if (FUTHARK_MAP.containsKey(lower)) {
                        sb.append(FUTHARK_MAP.get(lower));
                    } else {
                        sb.append(c);
                    }
                    break;
                case GOTHIC:
                    if (GOTHIC_MAP.containsKey(lower)) {
                        sb.append(GOTHIC_MAP.get(lower));
                    } else if (SGA_MAP.containsKey(lower)) {
                        sb.append(SGA_MAP.get(lower));
                    } else {
                        sb.append(c);
                    }
                    break;
                case ZALGO:
                    sb.append(c);
                    if (Character.isLetterOrDigit(c)) {
                        sb.append(ZALGO_MID[RNG.nextInt(ZALGO_MID.length)]);
                        sb.append(ZALGO_UP[RNG.nextInt(ZALGO_UP.length)]);
                    }
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    // Read-only views of the glyph tables, used by Decoder to build reverse maps.
    static Map<Character, String> sgaMap() { return SGA_MAP; }
    static Map<Character, String> futharkMap() { return FUTHARK_MAP; }
    static Map<Character, String> gothicMap() { return GOTHIC_MAP; }

    public static String getSgaDirect(char c) {
        char lower = Character.toLowerCase(c);
        return SGA_MAP.containsKey(lower) ? SGA_MAP.get(lower) : String.valueOf(c);
    }
}
