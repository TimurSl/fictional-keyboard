package com.enchanting.keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dota 2 demon language (Ozkavosh) word translator.
 *
 * The vocabulary is a fan-compiled list based on a developer post; a few glosses
 * (tho, hahsh, kish) are less certain. English and Russian words map to the same
 * demon words. Unknown words get a deterministic demonic rendering.
 */
public final class Ozkavosh {
    private Ozkavosh() {}

    // demon | english synonyms (first = primary gloss) | russian synonyms (first = primary gloss)
    private static final String[][] ENTRIES = {
        {"ozh", "i,me,my,mine", "я,меня,мой,мне,моя,моё,мои"},
        {"izh", "you,him,her,he,she,your,his", "ты,он,она,тебя,тебе,его,её,твой,вас,вы"},
        {"izhai", "they,them,group,their", "они,их,группа"},
        {"sa", "this", "это,этот"},
        {"vu", "that", "тот"},
        {"doq", "on", "на"},
        {"roq", "off", "прочь"},
        {"doz", "below,under", "под,ниже"},
        {"acha", "am,is,are,now,be", "сейчас,являюсь,является,быть"},
        {"icha", "will,could,would,can", "буду,будет,мог,может"},
        {"ucha", "was,had,were", "был,была,было,были,имел"},
        {"hollom", "without,nothing", "без,ничто,ничего"},
        {"tak", "take,have,has", "взять,брать,иметь,имею"},
        {"wroth", "again", "снова,опять"},
        {"lash", "part,share", "часть,доля"},
        {"alatho", "forward", "вперёд,вперед"},
        {"ulatho", "back", "назад"},
        {"tho", "at", "при"},
        {"sek", "come", "прийти,приходить,приди,иди"},
        {"thok", "go,move", "уйти,двигаться"},
        {"fek", "stop", "стоп,стой,остановить,остановиться"},
        {"ses", "hide", "прятать,скрыть,спрятать"},
        {"hahsh", "feel,touch", "чувствовать,трогать,касаться"},
        {"eyik", "look,behold", "смотреть,смотри,взгляни"},
        {"zomfa", "pleasure,happy", "удовольствие,счастье,счастливый"},
        {"domosh", "reign", "царствовать,правление"},
        {"arkosh", "master,king", "мастер,господин,король,владыка"},
        {"voth", "war,battle", "война,битва,бой"},
        {"hedoq", "desirable,delicious", "желанный,вкусный"},
        {"nith", "land,earth", "земля"},
        {"gluth", "eat,devour", "кушать,жрать,съесть,пожирать"},
        {"omoz", "darkness,hell,dark", "тьма,мрак,ад"},
        {"nesh", "home,safe", "дом,безопасный,безопасность"},
        {"safras", "pain", "боль"},
        {"poz", "power", "сила,мощь,власть"},
        {"irush", "illness,disease,sickness", "болезнь"},
        {"sol", "hope,light,sun,soul", "надежда,свет,солнце,душа"},
        {"groth", "spread,open", "распространять,открыть"},
        {"greesh", "debt", "долг"},
        {"lieyev", "payment,gift", "плата,оплата,дар,подарок"},
        {"chron", "time", "время"},
        {"rast", "toy", "игрушка"},
        {"miskath", "failure", "неудача,провал"},
        {"fol", "weakling,weak", "слабак,слабый"},
        {"ensh", "know,learn", "знать,узнать,учить"},
        {"ov", "one", "один"},
        {"sav", "seven", "семь"},
        {"kish", "waste", "отходы,трата"},
        {"sof", "curse", "проклятие,проклятье"},
        {"ashm", "more", "больше"},
        {"vohm", "less", "меньше"},
    };

    private static final String NOT_PREFIX = "vo'";
    private static final String VERY_PREFIX = "ahm'";

    private static final Map<String, String> TO_DEMON = new HashMap<>();
    private static final Map<String, String> DEMON_TO_EN = new HashMap<>();
    private static final Map<String, String> DEMON_TO_RU = new HashMap<>();

    static {
        for (String[] e : ENTRIES) {
            String[] en = e[1].split(",");
            String[] ru = e[2].split(",");
            for (String w : en) TO_DEMON.put(w, e[0]);
            for (String w : ru) TO_DEMON.put(w, e[0]);
            DEMON_TO_EN.put(e[0], en[0]);
            DEMON_TO_RU.put(e[0], ru[0]);
        }
    }

    private static final String[] EN_SUFFIXES = {"ing", "ed", "es", "s", "d"};
    private static final String[] RU_SUFFIXES = {"ом", "а", "ы", "и", "у", "ой", "е", "ю"};

    /** Returns "vo'" or "ahm'" when the word is a prefix modifier, else null. */
    public static String modifierPrefix(String word) {
        String w = normalize(word);
        switch (w) {
            case "not": case "no": case "не": case "нет":
                return NOT_PREFIX;
            case "very": case "очень":
                return VERY_PREFIX;
            default:
                return null;
        }
    }

    /** Looks a word up in the vocabulary, with basic stemming. Null if unknown. */
    static String lookup(String word) {
        String w = normalize(word);
        if (w.isEmpty()) return null;
        String hit = TO_DEMON.get(w);
        if (hit != null) return hit;
        for (String suf : EN_SUFFIXES) {
            if (w.length() > suf.length() + 1 && w.endsWith(suf)) {
                String stem = w.substring(0, w.length() - suf.length());
                hit = TO_DEMON.get(stem);
                if (hit == null) hit = TO_DEMON.get(stem + "e");
                if (hit == null && stem.length() > 2
                        && stem.charAt(stem.length() - 1) == stem.charAt(stem.length() - 2)) {
                    hit = TO_DEMON.get(stem.substring(0, stem.length() - 1));
                }
                if (hit != null) return hit;
            }
        }
        for (String suf : RU_SUFFIXES) {
            if (w.length() > suf.length() + 1 && w.endsWith(suf)) {
                String stem = w.substring(0, w.length() - suf.length());
                hit = TO_DEMON.get(stem);
                // genitive/plural endings hide the nominative vowel: войны -> война, земли -> земля
                if (hit == null) hit = TO_DEMON.get(stem + "а");
                if (hit == null) hit = TO_DEMON.get(stem + "я");
                if (hit == null) hit = TO_DEMON.get(stem + "ь");
                if (hit != null) return hit;
            }
        }
        return null;
    }

    /** Translates a single word (no modifier handling). Unknown words are rendered deterministically. */
    public static String translateWord(String word) {
        String hit = lookup(word);
        if (hit != null) return hit;
        return demonize(word);
    }

    /** Translates running text; keeps whitespace and punctuation, applies vo'/ahm' prefixes. */
    public static String translate(String text) {
        if (text == null || text.isEmpty()) return "";
        List<String> tokens = tokenize(text);
        StringBuilder out = new StringBuilder();
        String pending = null;
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            if (!isWord(t)) {
                // Whitespace between a modifier and its word is swallowed.
                if (pending != null && t.trim().isEmpty() && nextIsWord(tokens, i)) continue;
                if (pending != null) { out.append(pending.substring(0, pending.length() - 1)); pending = null; }
                out.append(t);
                continue;
            }
            String mod = modifierPrefix(t);
            if (mod != null) {
                if (pending != null) { out.append(pending); }
                pending = mod;
                continue;
            }
            if (pending != null) { out.append(pending); pending = null; }
            out.append(translateWord(t));
        }
        if (pending != null) out.append(pending.substring(0, pending.length() - 1));
        return out.toString();
    }

    /** Translates demon text back to English or Russian (target = true for Russian). */
    public static String decode(String text, boolean russian) {
        if (text == null || text.isEmpty()) return "";
        Map<String, String> dict = russian ? DEMON_TO_RU : DEMON_TO_EN;
        String not = russian ? "не " : "not ";
        String very = russian ? "очень " : "very ";
        StringBuilder out = new StringBuilder();
        for (String t : tokenize(text)) {
            if (!isWord(t)) { out.append(t); continue; }
            String w = t.toLowerCase().replace('’', '\'');
            String prefix = "";
            if (w.startsWith(NOT_PREFIX)) { prefix = not; w = w.substring(NOT_PREFIX.length()); }
            else if (w.startsWith(VERY_PREFIX)) { prefix = very; w = w.substring(VERY_PREFIX.length()); }
            else if (w.equals("vo")) { out.append(not.trim()); continue; }
            else if (w.equals("ahm")) { out.append(very.trim()); continue; }
            String gloss = dict.get(w);
            out.append(prefix).append(gloss != null ? gloss : w);
        }
        return out.toString();
    }

    /** Fraction of the words in the text that are valid demon words (0..1). */
    static double score(String text) {
        int words = 0, hits = 0;
        for (String t : tokenize(text)) {
            if (!isWord(t)) continue;
            words++;
            String w = t.toLowerCase().replace('’', '\'');
            if (w.startsWith(NOT_PREFIX)) w = w.substring(NOT_PREFIX.length());
            else if (w.startsWith(VERY_PREFIX)) w = w.substring(VERY_PREFIX.length());
            if (DEMON_TO_EN.containsKey(w)) hits++;
        }
        return words == 0 ? 0 : (double) hits / words;
    }

    /** Deterministic demonic rendering of an unknown word. */
    static String demonize(String word) {
        String w = Translit.toLatin(word).replace('’', '\'');
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (c == 'c') sb.append('k');
            else sb.append(c);
        }
        String r = sb.toString();
        r = r.replaceAll("([aeiu])\\1+", "o").replaceAll("o{2,}", "o");
        if (r.endsWith("s")) r = r.substring(0, r.length() - 1) + "zh";
        return r;
    }

    private static String normalize(String word) {
        if (word == null) return "";
        String w = word.toLowerCase().replace('’', '\'');
        while (w.endsWith("'")) w = w.substring(0, w.length() - 1);
        return w;
    }

    private static boolean isWord(String token) {
        return !token.isEmpty() && Translit.isWordChar(token.charAt(0));
    }

    private static boolean nextIsWord(List<String> tokens, int i) {
        return i + 1 < tokens.size() && isWord(tokens.get(i + 1));
    }

    static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean wc = Translit.isWordChar(c);
            if (cur.length() > 0 && wc != inWord) {
                tokens.add(cur.toString());
                cur.setLength(0);
            }
            inWord = wc;
            cur.append(c);
        }
        if (cur.length() > 0) tokens.add(cur.toString());
        return tokens;
    }
}
