package com.enchanting.keyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SgaKeyboardView extends LinearLayout {

    public interface OnKeyActionListener {
        void onText(String text);
        void onDelete();
        void onEnter();
        void onSwitchIme();
        void onShowImePicker();
        /** Raw word typed so far in a word-level mode, shown underlined; empty clears it. */
        void onComposing(String raw);
        /** Replaces the composing text with the translated word. */
        void onCommitWord(String translated);
        /** Currently selected text in the editor, or null. */
        String getSelectedText();
        String getTextBeforeCursor(int n);
    }

    private interface AltListener {
        void onAlternate(String alt);
    }

    private enum LayoutType {
        SGA_DIRECT,
        RU,
        EN,
        SYMBOLS
    }

    private static final long LONG_PRESS_MS = 350;

    private final OnKeyActionListener listener;
    private final Prefs prefs;
    private final Feedback feedback;
    private final KeyPopups popups;

    private Translators.Mode currentMode = Translators.Mode.SGA;
    private LayoutType currentLayout = LayoutType.SGA_DIRECT;
    private LayoutType layoutBeforeSymbols = LayoutType.SGA_DIRECT;
    private boolean isShifted = false;
    private int symPage = 0;

    private final StringBuilder composing = new StringBuilder();

    private LinearLayout headerBar;
    private LinearLayout keysContainer;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable repeatDeleteRunnable;

    // decode strip
    private LinearLayout decodeStrip;
    private TextView decodeInfo;
    private TextView decodeOut;
    private TextView decodeTargetBtn;
    private TextView decodeInsertBtn;
    private boolean decodeOpen = false;
    private String decodeSource = "";
    private boolean decodeFromSelection = false;
    private String decodedText = "";

    // Layout key definitions
    private static final String[][] SGA_ROWS = {
        {"ᑑ", "∴", "ᒷ", "ꋪ", "ℸ ̣", "||", "⚍", "╎", "𝙹", "!¡"},
        {"ᔑ", "ᓭ", "↸", "⎓", "⊣", "⍑", "⋮", "ꖌ", "ꖎ"},
        {"⨅", "⨉", "ᓵ", "⍊", "ʖ", "リ", "ᒲ"}
    };

    private static final String[][] SGA_SUB_LATIN = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"z", "x", "c", "v", "b", "n", "m"}
    };

    private static final String[][] RU_ROWS = {
        {"й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"},
        {"ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"},
        {"я", "ч", "с", "м", "и", "т", "ь", "б", "ю"}
    };

    private static final String[][] EN_ROWS = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"z", "x", "c", "v", "b", "n", "m"}
    };

    private static final String[][] SYM_ROWS = {
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
        {"@", "#", "$", "%", "&", "*", "-", "+", "(", ")"},
        {"!", "\"", "'", ":", ";", "/", "?", "~", "`"}
    };

    private static final String[][] SYM_ROWS_2 = {
        {"[", "]", "{", "}", "<", ">", "=", "_", "\\", "|"},
        {"^", "€", "£", "₽", "…", "«", "»", "¥", "°", "§"},
        {"¿", "¡", "©", "®", "™", "¢", "¶", "•", "·"}
    };

    private static final String[] DOT_ALTERNATES = {",", "?", "!", "'", "\"", "-", ":"};

    private static final Map<String, String[]> ACCENTS = new HashMap<>();
    static {
        ACCENTS.put("a", new String[]{"à", "á", "â", "ä", "ã", "å", "ā"});
        ACCENTS.put("e", new String[]{"è", "é", "ê", "ë", "ē"});
        ACCENTS.put("i", new String[]{"ì", "í", "î", "ï", "ī"});
        ACCENTS.put("o", new String[]{"ò", "ó", "ô", "ö", "õ", "ø", "ō"});
        ACCENTS.put("u", new String[]{"ù", "ú", "û", "ü", "ū"});
        ACCENTS.put("c", new String[]{"ç", "ć", "č"});
        ACCENTS.put("n", new String[]{"ñ", "ń"});
        ACCENTS.put("s", new String[]{"ß", "š", "ś"});
        ACCENTS.put("y", new String[]{"ý", "ÿ"});
        ACCENTS.put("z", new String[]{"ž", "ź", "ż"});
        ACCENTS.put("е", new String[]{"ё"});
        ACCENTS.put("ь", new String[]{"ъ"});
    }

    public SgaKeyboardView(Context context, OnKeyActionListener listener) {
        super(context);
        this.listener = listener;
        this.prefs = new Prefs(context);
        this.feedback = new Feedback(this, prefs);
        this.popups = new KeyPopups(this);

        restoreState();

        setOrientation(VERTICAL);
        setBackgroundColor(Color.parseColor("#18191C"));
        int pad = dp(4);
        setPadding(pad, pad, pad, dp(8));

        initHeader();
        initDecodeStrip();
        initKeysContainer();
        renderKeyboard();
    }

    private void restoreState() {
        try {
            String m = prefs.lastMode();
            if (m != null) currentMode = Translators.Mode.valueOf(m);
        } catch (IllegalArgumentException ignored) { }
        try {
            String l = prefs.lastLayout();
            if (l != null) {
                LayoutType t = LayoutType.valueOf(l);
                if (t != LayoutType.SYMBOLS) currentLayout = t;
            }
        } catch (IllegalArgumentException ignored) { }
        layoutBeforeSymbols = currentLayout;
    }

    // ---- Public hooks for the service ----

    public boolean hasComposing() {
        return composing.length() > 0;
    }

    /** Commits whatever word is being composed (translated). */
    public void flushComposing() {
        if (composing.length() == 0) return;
        String raw = composing.toString();
        composing.setLength(0);
        if (listener != null) listener.onCommitWord(translateWord(raw));
    }

    /** Drops the composing buffer without committing (the raw text stays in the editor). */
    public void clearComposing() {
        composing.setLength(0);
    }

    // ---- Header ----

    private void initHeader() {
        HorizontalScrollView hsv = new HorizontalScrollView(getContext());
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        headerBar = new LinearLayout(getContext());
        headerBar.setOrientation(HORIZONTAL);
        headerBar.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(4);
        headerBar.setPadding(pad, pad, pad, pad);

        hsv.addView(headerBar);
        addView(hsv);
    }

    private void initKeysContainer() {
        keysContainer = new LinearLayout(getContext());
        keysContainer.setOrientation(VERTICAL);
        keysContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(keysContainer);
    }

    private TextView createChip(String text, int bgColor, int textColor, int leftMarginDp) {
        TextView chip = new TextView(getContext());
        chip.setText(text);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(bgColor);
        chip.setBackground(bg);
        chip.setTextColor(textColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(leftMarginDp), 0, dp(3), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void refreshHeader() {
        headerBar.removeAllViews();

        for (final Translators.Mode mode : Translators.Mode.values()) {
            boolean isCurrent = (mode == currentMode);
            TextView chip = createChip(mode.title,
                Color.parseColor(isCurrent ? "#4C8BF5" : "#2A2D34"),
                isCurrent ? Color.WHITE : Color.parseColor("#A0A5B0"), 3);
            chip.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    feedback.key(Feedback.KeyType.SPECIAL);
                    flushComposing();
                    currentMode = mode;
                    prefs.setLastMode(mode.name());
                    renderKeyboard();
                }
            });
            headerBar.addView(chip);
        }

        String langLabel;
        if (currentLayout == LayoutType.SGA_DIRECT) {
            langLabel = "⌨ Руны";
        } else if (currentLayout == LayoutType.RU) {
            langLabel = "🌐 РУ";
        } else if (currentLayout == LayoutType.EN) {
            langLabel = "🌐 EN";
        } else {
            langLabel = "123";
        }
        TextView langChip = createChip(langLabel, Color.parseColor("#383E4A"), Color.parseColor("#76D4F5"), 8);
        langChip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                feedback.key(Feedback.KeyType.SPECIAL);
                flushComposing();
                if (currentLayout == LayoutType.SGA_DIRECT) {
                    currentLayout = LayoutType.RU;
                } else if (currentLayout == LayoutType.RU) {
                    currentLayout = LayoutType.EN;
                } else {
                    currentLayout = LayoutType.SGA_DIRECT;
                }
                layoutBeforeSymbols = currentLayout;
                prefs.setLastLayout(currentLayout.name());
                renderKeyboard();
            }
        });
        headerBar.addView(langChip);

        TextView decodeChip = createChip("⇄",
            Color.parseColor(decodeOpen ? "#4C8BF5" : "#383E4A"),
            decodeOpen ? Color.WHITE : Color.parseColor("#76D4F5"), 3);
        decodeChip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                feedback.key(Feedback.KeyType.SPECIAL);
                setDecodeOpen(!decodeOpen);
            }
        });
        headerBar.addView(decodeChip);
    }

    // ---- Decode strip ----

    private void initDecodeStrip() {
        decodeStrip = new LinearLayout(getContext());
        decodeStrip.setOrientation(VERTICAL);
        decodeStrip.setVisibility(GONE);
        decodeStrip.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E222A"));
        bg.setCornerRadius(dp(8));
        decodeStrip.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), dp(2), dp(4), dp(4));
        decodeStrip.setLayoutParams(lp);

        decodeInfo = new TextView(getContext());
        decodeInfo.setTextColor(Color.parseColor("#76D4F5"));
        decodeInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        decodeStrip.addView(decodeInfo);

        decodeOut = new TextView(getContext());
        decodeOut.setTextColor(Color.WHITE);
        decodeOut.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        decodeOut.setMaxLines(3);
        decodeOut.setEllipsize(android.text.TextUtils.TruncateAt.END);
        decodeOut.setPadding(0, dp(4), 0, dp(6));
        decodeStrip.addView(decodeOut);

        LinearLayout buttons = new LinearLayout(getContext());
        buttons.setOrientation(HORIZONTAL);

        decodeTargetBtn = decodeButton("", "#383E4A", new OnClickListener() {
            @Override
            public void onClick(View v) {
                Decoder.Target next;
                switch (prefs.decodeTarget()) {
                    case AUTO: next = Decoder.Target.EN; break;
                    case EN: next = Decoder.Target.RU; break;
                    default: next = Decoder.Target.AUTO; break;
                }
                prefs.setDecodeTarget(next);
                runDecode();
            }
        });
        buttons.addView(decodeTargetBtn);

        buttons.addView(decodeButton("Copy", "#383E4A", new OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && !decodedText.isEmpty()) {
                    cm.setPrimaryClip(ClipData.newPlainText("decoded", decodedText));
                    Toast.makeText(getContext(), "Copied", Toast.LENGTH_SHORT).show();
                }
            }
        }));

        decodeInsertBtn = decodeButton("Insert", "#3F69D6", new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!decodedText.isEmpty() && listener != null) {
                    flushComposing();
                    listener.onText(decodedText);
                    setDecodeOpen(false);
                }
            }
        });
        buttons.addView(decodeInsertBtn);

        buttons.addView(decodeButton("Close", "#383E4A", new OnClickListener() {
            @Override
            public void onClick(View v) {
                setDecodeOpen(false);
            }
        }));

        decodeStrip.addView(buttons);
        addView(decodeStrip);
    }

    private TextView decodeButton(String text, String color, OnClickListener click) {
        TextView b = new TextView(getContext());
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(color));
        bg.setCornerRadius(dp(8));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(lp);
        b.setOnClickListener(click);
        return b;
    }

    private void setDecodeOpen(boolean open) {
        decodeOpen = open;
        decodeStrip.setVisibility(open ? VISIBLE : GONE);
        if (open) loadDecodeSource();
        refreshHeader();
    }

    /** Picks what to decode: selection, then clipboard, then text before the cursor. */
    private void loadDecodeSource() {
        String sel = nz(listener != null ? listener.getSelectedText() : null);
        String clip = clipboardText();
        String before = nz(listener != null ? listener.getTextBeforeCursor(200) : null);
        String[] sources = {sel, clip, before};

        String chosen = null;
        for (String s : sources) {
            if (!s.trim().isEmpty() && Decoder.detect(s) != Decoder.Script.NONE) {
                chosen = s;
                break;
            }
        }
        if (chosen == null) {
            for (String s : sources) {
                if (!s.trim().isEmpty()) {
                    chosen = s;
                    break;
                }
            }
        }
        decodeSource = chosen != null ? chosen : "";
        decodeFromSelection = !sel.isEmpty() && decodeSource.equals(sel);
        runDecode();
    }

    private void runDecode() {
        Decoder.Target target = prefs.decodeTarget();
        Decoder.Result r = Decoder.decodeDetailed(decodeSource, Decoder.Script.AUTO, target);
        decodedText = r.text;
        decodeInfo.setText("Detected: " + r.script.label
            + (r.script == Decoder.Script.NONE ? "" : "  ·  read as " + r.target.label));
        decodeOut.setText(decodeSource.isEmpty() ? "Select glyph text or copy it, then press ⇄" : decodedText);
        decodeTargetBtn.setText("Lang: " + target.label);
        decodeInsertBtn.setText(decodeFromSelection ? "Replace" : "Insert");
    }

    private String clipboardText() {
        try {
            ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null
                    && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(getContext());
                return cs != null ? cs.toString() : "";
            }
        } catch (RuntimeException ignored) {
            // clipboard access can be denied
        }
        return "";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    // ---- Rendering ----

    public void renderKeyboard() {
        refreshHeader();
        keysContainer.removeAllViews();

        if (currentLayout == LayoutType.SYMBOLS) {
            renderSymbolsLayout();
        } else if (currentLayout == LayoutType.RU) {
            renderTextLayout(RU_ROWS, null);
        } else if (currentLayout == LayoutType.EN || currentMode.isWordLevel()) {
            // Word-level modes need real letters, so the SGA glyph layout shows Latin keys.
            renderTextLayout(EN_ROWS, null);
        } else {
            renderTextLayout(SGA_ROWS, SGA_SUB_LATIN);
        }

        renderBottomRow();
    }

    private boolean isGlyphLayout() {
        return currentLayout == LayoutType.SGA_DIRECT && !currentMode.isWordLevel();
    }

    private void renderTextLayout(String[][] rows, final String[][] subRows) {
        for (int r = 0; r < rows.length; r++) {
            LinearLayout rowView = createRowLayout();
            String[] rowKeys = rows[r];

            if (r == 2) {
                rowView.addView(createSpecialKey(isShifted ? "⬆" : "⇧", 1.4f, Color.parseColor("#343A45"),
                    Feedback.KeyType.SPECIAL, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            isShifted = !isShifted;
                            renderKeyboard();
                        }
                    }, null));
            }

            for (int k = 0; k < rowKeys.length; k++) {
                final String rawKey = rowKeys[k];
                final String sub = (subRows != null && r < subRows.length && k < subRows[r].length) ? subRows[r][k] : null;
                final boolean glyph = subRows != null;

                String displayKey = isShifted ? rawKey.toUpperCase() : rawKey;
                String hint = (r == 0 && k < 10) ? String.valueOf((k + 1) % 10) : null;

                // Alternates: digit first (top row), then layout-specific extras
                List<String> alts = new ArrayList<>();
                if (hint != null) alts.add(hint);
                if (glyph) {
                    alts.add(sub);
                    alts.add(sub.toUpperCase());
                } else {
                    String[] extra = ACCENTS.get(rawKey);
                    if (extra != null) Collections.addAll(alts, extra);
                }

                View keyView = createCharKey(displayKey, sub, hint, 1.0f, alts, isShifted,
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleCharInput(rawKey, glyph);
                        }
                    },
                    new AltListener() {
                        @Override
                        public void onAlternate(String alt) {
                            handleAlternate(alt, glyph);
                        }
                    });
                rowView.addView(keyView);
            }

            if (r == 2) {
                rowView.addView(createBackspaceKey(1.5f));
            }

            keysContainer.addView(rowView);
        }
    }

    private void renderSymbolsLayout() {
        String[][] rows = symPage == 0 ? SYM_ROWS : SYM_ROWS_2;
        for (int r = 0; r < rows.length; r++) {
            LinearLayout rowView = createRowLayout();
            String[] rowKeys = rows[r];

            if (r == 2) {
                rowView.addView(createSpecialKey(symPage == 0 ? "1/2" : "2/2", 1.2f, Color.parseColor("#343A45"),
                    Feedback.KeyType.SPECIAL, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            symPage = symPage == 0 ? 1 : 0;
                            renderKeyboard();
                        }
                    }, null));
            }

            for (final String finalKey : rowKeys) {
                rowView.addView(createCharKey(finalKey, null, null, 1.0f, null, false,
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            typeText(finalKey);
                        }
                    }, null));
            }

            if (r == 2) {
                rowView.addView(createBackspaceKey(1.3f));
            }

            keysContainer.addView(rowView);
        }
    }

    private void renderBottomRow() {
        LinearLayout rowView = createRowLayout();

        String toggleText = (currentLayout == LayoutType.SYMBOLS) ? "ABC" : "?123";
        rowView.addView(createSpecialKey(toggleText, 1.5f, Color.parseColor("#2F333B"),
            Feedback.KeyType.SPECIAL, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    flushComposing();
                    if (currentLayout == LayoutType.SYMBOLS) {
                        currentLayout = layoutBeforeSymbols;
                    } else {
                        layoutBeforeSymbols = currentLayout;
                        currentLayout = LayoutType.SYMBOLS;
                        symPage = 0;
                    }
                    renderKeyboard();
                }
            }, null));

        rowView.addView(createSpecialKey("🌐", 1.1f, Color.parseColor("#2F333B"),
            Feedback.KeyType.SPECIAL, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    flushComposing();
                    if (listener != null) listener.onSwitchIme();
                }
            }, null));

        String spaceLabel = "SGA • " + currentMode.title;
        rowView.addView(createSpecialKey(spaceLabel, 4.2f, Color.parseColor("#3D424D"),
            Feedback.KeyType.SPACE, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    typeText(" ");
                }
            }, new Runnable() {
                @Override
                public void run() {
                    flushComposing();
                    if (listener != null) listener.onShowImePicker();
                }
            }));

        // Dot key: long-press for common punctuation
        List<String> dotAlts = new ArrayList<>();
        Collections.addAll(dotAlts, DOT_ALTERNATES);
        rowView.addView(createCharKey(".", null, null, 1.0f, dotAlts, false,
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    typeText(".");
                }
            },
            new AltListener() {
                @Override
                public void onAlternate(String alt) {
                    typeText(alt);
                }
            }));

        rowView.addView(createSpecialKey("↵", 1.6f, Color.parseColor("#3F69D6"),
            Feedback.KeyType.ENTER, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    flushComposing();
                    if (listener != null) listener.onEnter();
                }
            }, null));

        keysContainer.addView(rowView);
    }

    // ---- Input handling ----

    private String translateWord(String raw) {
        switch (currentMode) {
            case DEMON: return Ozkavosh.translateWord(raw);
            case ARIRAL: return Ariral.translateWord(raw);
            default: return raw;
        }
    }

    /** Commits non-letter input (space, punctuation, digits), finishing any word being composed. */
    private void typeText(String text) {
        if (composing.length() > 0) {
            String raw = composing.toString();
            composing.setLength(0);
            String mod = currentMode == Translators.Mode.DEMON ? Ozkavosh.modifierPrefix(raw) : null;
            if (mod != null && text.equals(" ")) {
                // "not safe" -> "vo'nesh": the space after a modifier is swallowed so it attaches
                if (listener != null) listener.onCommitWord(mod);
                return;
            }
            if (listener != null) listener.onCommitWord(translateWord(raw));
        }
        if (listener != null) listener.onText(text);
    }

    private static boolean isLetter(String s) {
        return s.length() == 1 && Character.isLetter(s.charAt(0));
    }

    private void handleCharInput(String key, boolean glyphLayout) {
        if (currentMode.isWordLevel()) {
            if (isLetter(key)) {
                composing.append(isShifted ? key.toUpperCase() : key.toLowerCase());
                if (listener != null) listener.onComposing(composing.toString());
            } else {
                typeText(key);
            }
        } else {
            String toCommit;
            if (glyphLayout && currentLayout == LayoutType.SGA_DIRECT) {
                toCommit = key; // direct SGA glyph key
            } else {
                String letter = isShifted ? key.toUpperCase() : key.toLowerCase();
                toCommit = Translators.translateKey(letter, currentMode);
            }
            if (toCommit != null && !toCommit.isEmpty()) typeText(toCommit);
        }

        if (isShifted) {
            isShifted = false;
            renderKeyboard();
        }
    }

    /** A long-press choice: digits and symbols are literal; letters go through the active translation. */
    private void handleAlternate(String alt, boolean glyphLayout) {
        if (glyphLayout || !isLetter(alt)) {
            // On the SGA glyph layout the alternates are plain Latin letters / digits, typed as-is.
            String text = alt;
            if (isShifted && isLetter(alt)) text = alt.toUpperCase();
            typeText(text);
            if (isShifted) {
                isShifted = false;
                renderKeyboard();
            }
        } else {
            handleCharInput(alt, false);
        }
    }

    private void handleDelete() {
        if (composing.length() > 0) {
            composing.deleteCharAt(composing.length() - 1);
            if (listener != null) listener.onComposing(composing.toString());
        } else if (listener != null) {
            listener.onDelete();
        }
    }

    // ---- Key views ----

    private LinearLayout createRowLayout() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        row.setGravity(Gravity.CENTER);
        int margin = dp(2);
        row.setPadding(0, margin, 0, margin);
        return row;
    }

    private boolean isOutside(View v, MotionEvent e) {
        int slop = dp(16);
        return e.getX() < -slop || e.getY() < -slop || e.getX() > v.getWidth() + slop || e.getY() > v.getHeight() + slop;
    }

    /**
     * A letter/symbol key with a preview bubble, optional corner hint and long-press alternates.
     * Feedback fires on DOWN; the character is committed on UP (or the chosen alternate).
     */
    private View createCharKey(final String mainText, final String subText, final String hintText,
                               float weight, final List<String> alts, final boolean displayUpper,
                               final OnClickListener clickListener,
                               final AltListener altListener) {
        final FrameLayout keyLayout = new FrameLayout(getContext());

        final GradientDrawable normalBg = new GradientDrawable();
        normalBg.setCornerRadius(dp(6));
        normalBg.setColor(Color.parseColor("#2E323B"));

        final GradientDrawable pressedBg = new GradientDrawable();
        pressedBg.setCornerRadius(dp(6));
        pressedBg.setColor(Color.parseColor("#4D5463"));

        keyLayout.setBackground(normalBg);

        TextView tvMain = new TextView(getContext());
        tvMain.setText(mainText);
        tvMain.setTextColor(Color.WHITE);
        tvMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, mainText.length() > 2 ? 14 : 19);
        tvMain.setGravity(Gravity.CENTER);
        keyLayout.addView(tvMain, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        if (subText != null) {
            TextView tvSub = new TextView(getContext());
            tvSub.setText(subText);
            tvSub.setTextColor(Color.parseColor("#7A8291"));
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            slp.bottomMargin = dp(1);
            keyLayout.addView(tvSub, slp);
        }

        if (hintText != null) {
            TextView tvHint = new TextView(getContext());
            tvHint.setText(hintText);
            tvHint.setTextColor(Color.parseColor("#8E99A8"));
            tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
            hlp.topMargin = dp(2);
            hlp.rightMargin = dp(4);
            keyLayout.addView(tvHint, hlp);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), weight);
        int m = dp(2);
        lp.setMargins(m, m, m, m);
        keyLayout.setLayoutParams(lp);

        // Alternates shown uppercase while shifted (digits/symbols are unaffected)
        final List<String> shownAlts = new ArrayList<>();
        if (alts != null) {
            for (String a : alts) shownAlts.add(displayUpper ? a.toUpperCase() : a);
        }
        final boolean hasAlts = !shownAlts.isEmpty() && altListener != null;
        final boolean preview = !mainText.trim().isEmpty() && mainText.length() <= 3;

        keyLayout.setOnTouchListener(new OnTouchListener() {
            private boolean altActive = false;
            private Runnable longPress;

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        altActive = false;
                        keyLayout.setBackground(pressedBg);
                        feedback.key(Feedback.KeyType.LETTER);
                        if (preview) popups.showPreview(keyLayout, mainText);
                        if (hasAlts) {
                            longPress = new Runnable() {
                                @Override
                                public void run() {
                                    altActive = true;
                                    popups.hidePreview();
                                    popups.showAlternates(keyLayout, shownAlts);
                                    feedback.key(Feedback.KeyType.POPUP_OPEN);
                                }
                            };
                            uiHandler.postDelayed(longPress, LONG_PRESS_MS);
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (altActive) popups.highlightAt(event.getRawX());
                        break;

                    case MotionEvent.ACTION_UP:
                        cancelLongPress();
                        keyLayout.setBackground(normalBg);
                        if (altActive) {
                            int idx = popups.highlightAt(event.getRawX());
                            popups.dismissAll();
                            altActive = false;
                            // commit the un-uppercased alternate; the handler applies shift itself
                            if (idx >= 0 && idx < alts.size()) altListener.onAlternate(alts.get(idx));
                        } else {
                            popups.hidePreview();
                            if (!isOutside(v, event) && clickListener != null) clickListener.onClick(v);
                        }
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        cancelLongPress();
                        keyLayout.setBackground(normalBg);
                        altActive = false;
                        popups.dismissAll();
                        break;
                }
                return true;
            }

            private void cancelLongPress() {
                if (longPress != null) {
                    uiHandler.removeCallbacks(longPress);
                    longPress = null;
                }
            }
        });

        return keyLayout;
    }

    private View createSpecialKey(String text, float weight, final int bgColor, final Feedback.KeyType type,
                                  final OnClickListener clickListener, final Runnable onLongPress) {
        final TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);

        final GradientDrawable normalBg = new GradientDrawable();
        normalBg.setCornerRadius(dp(6));
        normalBg.setColor(bgColor);

        final GradientDrawable pressedBg = new GradientDrawable();
        pressedBg.setCornerRadius(dp(6));
        pressedBg.setColor(Color.parseColor("#5A6273"));

        tv.setBackground(normalBg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), weight);
        int m = dp(2);
        lp.setMargins(m, m, m, m);
        tv.setLayoutParams(lp);

        tv.setOnTouchListener(new OnTouchListener() {
            private boolean longFired = false;
            private Runnable longPress;

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        longFired = false;
                        tv.setBackground(pressedBg);
                        feedback.key(type);
                        if (onLongPress != null) {
                            longPress = new Runnable() {
                                @Override
                                public void run() {
                                    longFired = true;
                                    feedback.key(Feedback.KeyType.POPUP_OPEN);
                                    onLongPress.run();
                                }
                            };
                            uiHandler.postDelayed(longPress, LONG_PRESS_MS);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        cancelLongPress();
                        tv.setBackground(normalBg);
                        if (!longFired && !isOutside(v, event) && clickListener != null) clickListener.onClick(v);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        cancelLongPress();
                        tv.setBackground(normalBg);
                        break;
                }
                return true;
            }

            private void cancelLongPress() {
                if (longPress != null) {
                    uiHandler.removeCallbacks(longPress);
                    longPress = null;
                }
            }
        });

        return tv;
    }

    private View createBackspaceKey(float weight) {
        final TextView tv = new TextView(getContext());
        tv.setText("⌫");
        tv.setTextColor(Color.parseColor("#FF8A80"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);

        final GradientDrawable normalBg = new GradientDrawable();
        normalBg.setCornerRadius(dp(6));
        normalBg.setColor(Color.parseColor("#343A45"));

        final GradientDrawable pressedBg = new GradientDrawable();
        pressedBg.setCornerRadius(dp(6));
        pressedBg.setColor(Color.parseColor("#5A6273"));

        tv.setBackground(normalBg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), weight);
        int m = dp(2);
        lp.setMargins(m, m, m, m);
        tv.setLayoutParams(lp);

        tv.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        tv.setBackground(pressedBg);
                        feedback.key(Feedback.KeyType.DELETE);
                        handleDelete();

                        stopRepeat();
                        repeatDeleteRunnable = new Runnable() {
                            @Override
                            public void run() {
                                handleDelete();
                                feedback.key(Feedback.KeyType.DELETE_REPEAT);
                                uiHandler.postDelayed(this, 60);
                            }
                        };
                        uiHandler.postDelayed(repeatDeleteRunnable, 400);
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        tv.setBackground(normalBg);
                        stopRepeat();
                        break;
                }
                return true;
            }
        });

        return tv;
    }

    private void stopRepeat() {
        if (repeatDeleteRunnable != null) {
            uiHandler.removeCallbacks(repeatDeleteRunnable);
            repeatDeleteRunnable = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopRepeat();
        popups.dismissAll();
    }

    private int dp(float dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }
}
