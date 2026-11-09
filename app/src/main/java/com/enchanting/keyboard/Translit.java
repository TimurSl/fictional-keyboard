package com.enchanting.keyboard;

/** Cyrillic to Latin transliteration shared by the word-level game-language translators. */
final class Translit {
    private Translit() {}

    private static final String CYR = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String[] LAT = {
        "a", "b", "v", "g", "d", "e", "yo", "zh", "z", "i", "y", "k", "l", "m", "n", "o", "p",
        "r", "s", "t", "u", "f", "kh", "ts", "ch", "sh", "shch", "", "y", "", "e", "yu", "ya"
    };

    static boolean isCyrillic(char c) {
        char l = Character.toLowerCase(c);
        return CYR.indexOf(l) >= 0;
    }

    /** Lowercases and converts Cyrillic letters to Latin; other characters pass through. */
    static String toLatin(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            int idx = CYR.indexOf(c);
            if (idx >= 0) sb.append(LAT[idx]);
            else sb.append(c);
        }
        return sb.toString();
    }

    /** True for characters that can be part of a word (letters and inner apostrophes). */
    static boolean isWordChar(char c) {
        return Character.isLetter(c) || c == '\'' || c == '’';
    }
}
