package com.enchanting.keyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The keyboard UI, styled after the Samsung keyboard: a round-button toolbar on top, a
 * permanent number row and symbol hints in the corner of every letter key.
 */
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
        /** Opens the keyboard settings screen. */
        void onOpenSettings();
    }

    private interface AltListener {
        void onAlternate(String alt);
    }

    private enum LayoutType {
        SGA_DIRECT,
        RU,
        EN,
        SYMBOLS,
        EMOJI
    }

    // Palette sampled from the Samsung keyboard in dark mode.
    private static final int C_BOARD = Color.parseColor("#0D0D0E");
    private static final int C_TOOLBAR = Color.parseColor("#1C1C1D");
    private static final int C_CIRCLE = Color.parseColor("#3A3A3C");
    private static final int C_KEY = Color.parseColor("#2F2E2C");
    private static final int C_KEY_FUNC = Color.parseColor("#201F1E");
    private static final int C_TEXT = Color.parseColor("#F5F5F5");
    private static final int C_HINT = Color.parseColor("#D9D9D9");
    private static final int C_SUB = Color.parseColor("#8C8C8C");
    private static final int C_ACCENT = Color.parseColor("#3E91FF");
    private static final int C_PANEL = Color.parseColor("#1F1F20");

    private static final long LONG_PRESS_MS = 350;
    private static final long CAPS_DOUBLE_TAP_MS = 400;

    // Row geometry (dp), like the Samsung keyboard: tall keys with generous gaps.
    private static final float H_NUMBER = 36f;
    private static final float H_LETTER = 42f;
    private static final float SIDE_W = 1.5f;

    // Visual gap around each key plate (dp). It is drawn as an inset inside the key view rather
    // than as a margin, so a tap that lands in the gap still hits the nearest key.
    private static final float GAP_H = 3f;
    private static final float GAP_V = 5f;

    private final OnKeyActionListener listener;
    private final Prefs prefs;
    private final Feedback feedback;
    private final KeyPopups popups;

    private Translators.Mode currentMode = Translators.Mode.SGA;
    private LayoutType currentLayout = LayoutType.SGA_DIRECT;
    private LayoutType layoutBeforeSymbols = LayoutType.SGA_DIRECT;
    private boolean isShifted = false;
    private boolean capsLock = false;
    private long lastShiftTap = 0;
    private int symPage = 0;
    private boolean modeStripOpen = false;

    private final StringBuilder composing = new StringBuilder();

    private LinearLayout toolbar;
    private HorizontalScrollView modeStrip;
    private LinearLayout modeRow;
    private LinearLayout keysContainer;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable repeatDeleteRunnable;

    // Number of keys currently held down, and whether a re-render is waiting for them.
    private int keysDown = 0;
    private boolean renderPending = false;

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
        {"й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х"},
        {"ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"},
        {"я", "ч", "с", "м", "и", "т", "ь", "б", "ю"}
    };

    private static final String[][] EN_ROWS = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"z", "x", "c", "v", "b", "n", "m"}
    };

    /** Corner symbols on the Latin letters, exactly as on the Samsung keyboard. */
    private static final String[][] HINT_ROWS = {
        {"+", "×", "÷", "=", "/", "_", "<", ">", "[", "]"},
        {"!", "@", "#", "%", "^", "&", "*", "(", ")"},
        {"-", "'", "\"", ":", ";", ",", "?"}
    };

    /** Russian corner symbols; ё and ъ live on е and ь so the rows stay at 11 keys. */
    private static final String[][] HINT_ROWS_RU = {
        {"+", "×", "÷", "=", "ё", "/", "_", "<", ">", "[", "]"},
        {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "~"},
        {"-", "'", "\"", ":", ";", ",", "ъ", "?", "`"}
    };

    private static final String[] DIGITS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};

    private static final String[][] SYM_ROWS = {
        {"+", "×", "÷", "=", "/", "_", "<", ">", "[", "]"},
        {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")"},
        {"-", "'", "\"", ":", ";", ",", "?"}
    };

    private static final String[][] SYM_ROWS_2 = {
        {"`", "~", "\\", "|", "{", "}", "€", "£", "¥", "₽"},
        {"…", "«", "»", "§", "¶", "©", "®", "™", "¢", "°"},
        {"¿", "¡", "•", "·", "†", "‡", "№"}
    };

    private static final String[][] EMOJI_ROWS = {
        {"😀", "😂", "😊", "😍", "😘", "😎", "🤔", "😭"},
        {"😡", "😴", "😱", "👍", "👎", "👏", "🙏", "💪"},
        {"❤️", "🔥", "✨", "⭐", "🎉", "💀", "👀", "💯"},
        {"😈", "👹", "👽", "🤖", "🌙", "⚔️", "⛏️"}
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
    }

    public SgaKeyboardView(Context context, OnKeyActionListener listener) {
        super(context);
        this.listener = listener;
        this.prefs = new Prefs(context);
        this.feedback = new Feedback(this, prefs);
        this.popups = new KeyPopups(this);

        restoreState();

        setOrientation(VERTICAL);
        setBackgroundColor(C_BOARD);

        initToolbar();
        initModeStrip();
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
                if (t != LayoutType.SYMBOLS && t != LayoutType.EMOJI) currentLayout = t;
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

    // ---- Toolbar (round icon buttons, like the strip above the Samsung keys) ----

    private void initToolbar() {
        toolbar = new LinearLayout(getContext());
        toolbar.setOrientation(HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackgroundColor(C_TOOLBAR);
        toolbar.setPadding(dp(10), dp(6), dp(10), dp(6));
        toolbar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp(54)));
        addView(toolbar);
    }

    private void initModeStrip() {
        modeStrip = new HorizontalScrollView(getContext());
        modeStrip.setHorizontalScrollBarEnabled(false);
        modeStrip.setVisibility(GONE);
        modeStrip.setBackgroundColor(C_TOOLBAR);
        modeStrip.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        modeRow = new LinearLayout(getContext());
        modeRow.setOrientation(HORIZONTAL);
        modeRow.setGravity(Gravity.CENTER_VERTICAL);
        modeRow.setPadding(dp(10), 0, dp(10), dp(8));
        modeStrip.addView(modeRow);
        addView(modeStrip);
    }

    private void initKeysContainer() {
        keysContainer = new LinearLayout(getContext());
        keysContainer.setOrientation(VERTICAL);
        keysContainer.setPadding(dp(4), dp(8), dp(4), dp(10));
        keysContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(keysContainer);
    }

    private ImageView icon(int res, float sizeDp) {
        ImageView iv = new ImageView(getContext());
        iv.setImageResource(res);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        iv.setLayoutParams(new FrameLayout.LayoutParams(dp(sizeDp), dp(sizeDp), Gravity.CENTER));
        return iv;
    }

    private View toolbarButton(int iconRes, String description, boolean active, OnClickListener click) {
        FrameLayout b = new FrameLayout(getContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(active ? C_ACCENT : C_CIRCLE);
        b.setBackground(bg);
        b.setContentDescription(description);
        b.setOnClickListener(click);
        b.addView(icon(iconRes, 22f));
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        return b;
    }

    private View toolbarSpacer() {
        View v = new View(getContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    private void refreshToolbar() {
        toolbar.removeAllViews();

        toolbar.addView(toolbarSpacer());
        toolbar.addView(toolbarButton(R.drawable.ic_sparkle, "Mode: " + currentMode.label, modeStripOpen,
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    feedback.key(Feedback.KeyType.SPECIAL);
                    modeStripOpen = !modeStripOpen;
                    refreshToolbar();
                }
            }));
        toolbar.addView(toolbarSpacer());
        toolbar.addView(toolbarButton(R.drawable.ic_emoji, "Emoji", currentLayout == LayoutType.EMOJI,
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    feedback.key(Feedback.KeyType.SPECIAL);
                    toggleEmoji();
                }
            }));
        toolbar.addView(toolbarSpacer());
        toolbar.addView(toolbarButton(R.drawable.ic_translate, "Switch layout: " + layoutName(baseLayout()), false,
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    feedback.key(Feedback.KeyType.SPECIAL);
                    flushComposing();
                    LayoutType base = baseLayout();
                    if (base == LayoutType.SGA_DIRECT) {
                        base = LayoutType.RU;
                    } else if (base == LayoutType.RU) {
                        base = LayoutType.EN;
                    } else {
                        base = LayoutType.SGA_DIRECT;
                    }
                    currentLayout = base;
                    layoutBeforeSymbols = base;
                    prefs.setLastLayout(base.name());
                    requestRender();
                }
            }));
        toolbar.addView(toolbarSpacer());
        toolbar.addView(toolbarButton(R.drawable.ic_swap, "Decode text", decodeOpen, new OnClickListener() {
            @Override
            public void onClick(View v) {
                feedback.key(Feedback.KeyType.SPECIAL);
                setDecodeOpen(!decodeOpen);
            }
        }));
        toolbar.addView(toolbarSpacer());
        toolbar.addView(toolbarButton(R.drawable.ic_settings, "Settings", false, new OnClickListener() {
            @Override
            public void onClick(View v) {
                feedback.key(Feedback.KeyType.SPECIAL);
                flushComposing();
                if (listener != null) listener.onOpenSettings();
            }
        }));
        toolbar.addView(toolbarSpacer());

        refreshModeStrip();
    }

    private void refreshModeStrip() {
        modeStrip.setVisibility(modeStripOpen ? VISIBLE : GONE);
        modeRow.removeAllViews();
        if (!modeStripOpen) return;

        for (final Translators.Mode mode : Translators.Mode.values()) {
            boolean isCurrent = (mode == currentMode);
            TextView chip = new TextView(getContext());
            chip.setText(mode.title);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setTextColor(isCurrent ? Color.WHITE : Color.parseColor("#C8C8C8"));
            chip.setPadding(dp(14), 0, dp(14), 0);
            chip.setBackground(rounded(isCurrent ? C_ACCENT : C_CIRCLE, 18));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, dp(36));
            lp.setMargins(dp(3), 0, dp(3), 0);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    feedback.key(Feedback.KeyType.SPECIAL);
                    flushComposing();
                    currentMode = mode;
                    prefs.setLastMode(mode.name());
                    modeStripOpen = false;
                    requestRender();
                }
            });
            modeRow.addView(chip);
        }
    }

    private void toggleEmoji() {
        flushComposing();
        if (currentLayout == LayoutType.EMOJI) {
            currentLayout = layoutBeforeSymbols;
        } else {
            if (currentLayout != LayoutType.SYMBOLS) layoutBeforeSymbols = currentLayout;
            currentLayout = LayoutType.EMOJI;
        }
        requestRender();
    }

    /** The text layout underneath the symbol/emoji panels. */
    private LayoutType baseLayout() {
        return (currentLayout == LayoutType.SYMBOLS || currentLayout == LayoutType.EMOJI)
            ? layoutBeforeSymbols : currentLayout;
    }

    private static String layoutName(LayoutType t) {
        switch (t) {
            case RU: return "Русский";
            case EN: return "English";
            default: return "Galactic";
        }
    }

    /** Space bar caption, the way the Samsung one shows "English (US)". */
    private String spaceLabel() {
        LayoutType base = baseLayout();
        boolean glyphs = base == LayoutType.SGA_DIRECT && !currentMode.isWordLevel();
        if (glyphs) return "Galactic (SGA)";
        String lang = base == LayoutType.RU ? "Русский" : "English";
        return currentMode == Translators.Mode.NORMAL ? lang : lang + " · " + currentMode.label;
    }

    // ---- Decode strip ----

    private void initDecodeStrip() {
        decodeStrip = new LinearLayout(getContext());
        decodeStrip.setOrientation(VERTICAL);
        decodeStrip.setVisibility(GONE);
        decodeStrip.setPadding(dp(12), dp(10), dp(12), dp(10));
        decodeStrip.setBackground(rounded(C_PANEL, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(8), dp(8), dp(8), 0);
        decodeStrip.setLayoutParams(lp);

        decodeInfo = new TextView(getContext());
        decodeInfo.setTextColor(Color.parseColor("#8DBBFF"));
        decodeInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        decodeStrip.addView(decodeInfo);

        decodeOut = new TextView(getContext());
        decodeOut.setTextColor(C_TEXT);
        decodeOut.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        decodeOut.setMaxLines(3);
        decodeOut.setEllipsize(android.text.TextUtils.TruncateAt.END);
        decodeOut.setPadding(0, dp(4), 0, dp(8));
        decodeStrip.addView(decodeOut);

        LinearLayout buttons = new LinearLayout(getContext());
        buttons.setOrientation(HORIZONTAL);

        decodeTargetBtn = decodeButton("", C_CIRCLE, new OnClickListener() {
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

        buttons.addView(decodeButton("Copy", C_CIRCLE, new OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && !decodedText.isEmpty()) {
                    cm.setPrimaryClip(ClipData.newPlainText("decoded", decodedText));
                    Toast.makeText(getContext(), "Copied", Toast.LENGTH_SHORT).show();
                }
            }
        }));

        decodeInsertBtn = decodeButton("Insert", C_ACCENT, new OnClickListener() {
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

        buttons.addView(decodeButton("Close", C_CIRCLE, new OnClickListener() {
            @Override
            public void onClick(View v) {
                setDecodeOpen(false);
            }
        }));

        decodeStrip.addView(buttons);
        addView(decodeStrip);
    }

    private TextView decodeButton(String text, int color, OnClickListener click) {
        TextView b = new TextView(getContext());
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(color, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(lp);
        b.setOnClickListener(click);
        return b;
    }

    private void setDecodeOpen(boolean open) {
        decodeOpen = open;
        decodeStrip.setVisibility(open ? VISIBLE : GONE);
        if (open) loadDecodeSource();
        refreshToolbar();
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

    /**
     * Rebuilds the keys, but never while a finger is still on one: tearing the views down
     * cancels that touch and the key the user pressed is silently lost (e.g. the second
     * letter of a fast "He" after a one-shot shift). The rebuild waits for the last release.
     */
    private void requestRender() {
        renderPending = true;
        if (keysDown == 0) uiHandler.post(pendingRender);
    }

    private final Runnable pendingRender = new Runnable() {
        @Override
        public void run() {
            if (renderPending && keysDown == 0) renderKeyboard();
        }
    };

    private void keyPressed() {
        keysDown++;
    }

    private void keyReleased() {
        if (keysDown > 0) keysDown--;
        if (keysDown == 0 && renderPending) uiHandler.post(pendingRender);
    }

    public void renderKeyboard() {
        renderPending = false;
        uiHandler.removeCallbacks(pendingRender);
        refreshToolbar();
        keysContainer.removeAllViews();

        if (currentLayout == LayoutType.EMOJI) {
            renderEmojiLayout();
        } else {
            renderNumberRow();
            if (currentLayout == LayoutType.SYMBOLS) {
                renderSymbolsLayout();
            } else if (currentLayout == LayoutType.RU) {
                renderTextLayout(RU_ROWS, null, HINT_ROWS_RU, 21);
            } else if (currentLayout == LayoutType.EN || currentMode.isWordLevel()) {
                // Word-level modes need real letters, so the SGA glyph layout shows Latin keys.
                renderTextLayout(EN_ROWS, null, HINT_ROWS, 22);
            } else {
                renderTextLayout(SGA_ROWS, SGA_SUB_LATIN, HINT_ROWS, 20);
            }
        }

        renderBottomRow();
    }

    private void renderNumberRow() {
        LinearLayout rowView = createRowLayout();
        for (final String d : DIGITS) {
            rowView.addView(createCharKey(d, null, null, 1.0f, H_NUMBER, 20, null, false,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        typeText(d);
                    }
                }, null));
        }
        keysContainer.addView(rowView);
    }

    private void renderTextLayout(String[][] rows, final String[][] subRows, String[][] hints, float textSp) {
        for (int r = 0; r < rows.length; r++) {
            LinearLayout rowView = createRowLayout();
            String[] rowKeys = rows[r];
            // Shorter rows are centred (row 2) or flanked by shift/backspace (row 3).
            float side = (rows[0].length - rowKeys.length) / 2f;

            if (r == 1 && side > 0) rowView.addView(rowSpacer(side, null));

            if (r == 2) {
                int shiftIcon = capsLock ? R.drawable.ic_caps : (isShifted ? R.drawable.ic_shift_on : R.drawable.ic_shift);
                rowView.addView(createFuncKey(null, shiftIcon, "Shift", side, C_KEY_FUNC,
                    Feedback.KeyType.SPECIAL, H_LETTER, 0, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onShiftTap();
                        }
                    }, null));
            }

            for (int k = 0; k < rowKeys.length; k++) {
                final String rawKey = rowKeys[k];
                final String sub = (subRows != null && r < subRows.length && k < subRows[r].length) ? subRows[r][k] : null;
                final boolean glyph = subRows != null;
                final String hint = (hints != null && r < hints.length && k < hints[r].length) ? hints[r][k] : null;

                String displayKey = isShifted ? rawKey.toUpperCase() : rawKey;

                // Long-press choices: the corner symbol first (release to type it), then extras.
                List<String> alts = new ArrayList<>();
                if (hint != null) alts.add(hint);
                if (glyph) {
                    alts.add(sub);
                    alts.add(sub.toUpperCase());
                } else {
                    String[] extra = ACCENTS.get(rawKey);
                    if (extra != null) Collections.addAll(alts, extra);
                }

                View keyView = createCharKey(displayKey, sub, hint, 1.0f, H_LETTER, textSp, alts, isShifted,
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

            if (r == 1 && side > 0) {
                // The empty space beside a centred row belongs to its outermost keys.
                int firstKey = 1; // index 0 is the left spacer
                int lastKey = rowView.getChildCount() - 1;
                rowView.addView(rowSpacer(side, rowView.getChildAt(lastKey)));
                ((TouchForwarder) rowView.getChildAt(0)).target = rowView.getChildAt(firstKey);
            }
            if (r == 2) rowView.addView(createBackspaceKey(side, H_LETTER));

            keysContainer.addView(rowView);
        }
    }

    private void onShiftTap() {
        long now = SystemClock.uptimeMillis();
        if (capsLock) {
            capsLock = false;
            isShifted = false;
        } else if (isShifted && now - lastShiftTap < CAPS_DOUBLE_TAP_MS) {
            capsLock = true; // double tap
        } else {
            isShifted = !isShifted;
        }
        lastShiftTap = now;
        requestRender();
    }

    private void renderSymbolsLayout() {
        String[][] rows = symPage == 0 ? SYM_ROWS : SYM_ROWS_2;
        for (int r = 0; r < rows.length; r++) {
            LinearLayout rowView = createRowLayout();
            String[] rowKeys = rows[r];
            float side = (rows[0].length - rowKeys.length) / 2f;

            if (r == 2) {
                rowView.addView(createFuncKey(symPage == 0 ? "1/2" : "2/2", 0, "More symbols", side,
                    C_KEY_FUNC, Feedback.KeyType.SPECIAL, H_LETTER, 15, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            symPage = symPage == 0 ? 1 : 0;
                            requestRender();
                        }
                    }, null));
            }

            for (final String finalKey : rowKeys) {
                rowView.addView(createCharKey(finalKey, null, null, 1.0f, H_LETTER, 22, null, false,
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            typeText(finalKey);
                        }
                    }, null));
            }

            if (r == 2) rowView.addView(createBackspaceKey(side, H_LETTER));

            keysContainer.addView(rowView);
        }
    }

    private void renderEmojiLayout() {
        float h = (H_NUMBER + 3 * H_LETTER) / EMOJI_ROWS.length;
        for (int r = 0; r < EMOJI_ROWS.length; r++) {
            LinearLayout rowView = createRowLayout();
            for (final String e : EMOJI_ROWS[r]) {
                rowView.addView(createEmojiKey(e, h));
            }
            if (r == EMOJI_ROWS.length - 1) {
                rowView.addView(createBackspaceKey(1f, h));
            }
            keysContainer.addView(rowView);
        }
    }

    private void renderBottomRow() {
        LinearLayout rowView = createRowLayout();

        boolean panel = currentLayout == LayoutType.SYMBOLS || currentLayout == LayoutType.EMOJI;
        rowView.addView(createFuncKey(panel ? "ABC" : "!#1", 0, panel ? "Letters" : "Symbols", 1.5f, C_KEY,
            Feedback.KeyType.SPECIAL, H_LETTER, 17, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    flushComposing();
                    if (currentLayout == LayoutType.SYMBOLS || currentLayout == LayoutType.EMOJI) {
                        currentLayout = layoutBeforeSymbols;
                    } else {
                        layoutBeforeSymbols = currentLayout;
                        currentLayout = LayoutType.SYMBOLS;
                        symPage = 0;
                    }
                    requestRender();
                }
            }, null));

        rowView.addView(createFuncKey(null, R.drawable.ic_globe, "Switch keyboard", 1.0f, C_KEY,
            Feedback.KeyType.SPECIAL, H_LETTER, 0, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    flushComposing();
                    if (listener != null) listener.onSwitchIme();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    flushComposing();
                    if (listener != null) listener.onShowImePicker();
                }
            }));

        rowView.addView(createFuncKey(null, R.drawable.ic_emoji, "Emoji", 1.0f, C_KEY,
            Feedback.KeyType.SPECIAL, H_LETTER, 0, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleEmoji();
                }
            }, null));

        rowView.addView(createFuncKey(spaceLabel(), 0, "Space", 3.9f, C_KEY,
            Feedback.KeyType.SPACE, H_LETTER, 15, new OnClickListener() {
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
        rowView.addView(createCharKey(".", null, null, 1.0f, H_LETTER, 26, dotAlts, false,
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

        rowView.addView(createFuncKey(null, R.drawable.ic_enter, "Enter", 1.6f, C_KEY,
            Feedback.KeyType.ENTER, H_LETTER, 0, new OnClickListener() {
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

    /** Drops one-shot shift after a character; caps lock stays on. */
    private void consumeShift() {
        if (isShifted && !capsLock) {
            isShifted = false;
            requestRender();
        }
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

        consumeShift();
    }

    /** A long-press choice: digits and symbols are literal; letters go through the active translation. */
    private void handleAlternate(String alt, boolean glyphLayout) {
        if (glyphLayout || !isLetter(alt)) {
            // On the SGA glyph layout the alternates are plain Latin letters / symbols, typed as-is.
            String text = alt;
            if (isShifted && isLetter(alt)) text = alt.toUpperCase();
            typeText(text);
            consumeShift();
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
        return row;
    }

    /** Empty space in a row that hands its touches to a neighbouring key. */
    private final class TouchForwarder extends View {
        View target;

        TouchForwarder(Context context, View target) {
            super(context);
            this.target = target;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (target == null) return false;
            MotionEvent e = MotionEvent.obtain(event);
            e.offsetLocation(getLeft() - target.getLeft(), getTop() - target.getTop());
            target.dispatchTouchEvent(e);
            e.recycle();
            return true;
        }
    }

    private View rowSpacer(float weight, View target) {
        View v = new TouchForwarder(getContext(), target);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, weight));
        return v;
    }

    /** Key size including the gap around its plate; the gap stays touchable. */
    private LinearLayout.LayoutParams keyParams(float weight, float heightDp) {
        return new LinearLayout.LayoutParams(0, dp(heightDp + 2 * GAP_V), weight);
    }

    /** A rounded key plate, inset so the visible gap between keys is part of the key's touch area. */
    private Drawable plate(int color) {
        int h = dp(GAP_H);
        int v = dp(GAP_V);
        return new InsetDrawable(rounded(color, 8), h, v, h, v);
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
                               float weight, float heightDp, float textSp, final List<String> alts,
                               final boolean displayUpper,
                               final OnClickListener clickListener,
                               final AltListener altListener) {
        final FrameLayout keyLayout = new FrameLayout(getContext());

        final Drawable normalBg = plate(C_KEY);
        final Drawable pressedBg = plate(lighten(C_KEY, 0.35f));
        keyLayout.setBackground(normalBg);

        TextView tvMain = new TextView(getContext());
        tvMain.setText(mainText);
        tvMain.setTextColor(C_TEXT);
        tvMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, mainText.length() > 2 ? 14 : textSp);
        tvMain.setGravity(Gravity.CENTER);
        tvMain.setIncludeFontPadding(false);
        FrameLayout.LayoutParams mlp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        // The letter sits a little low so the corner hint has room, as on the Samsung keys.
        if (hintText != null) mlp.topMargin = dp(5);
        keyLayout.addView(tvMain, mlp);

        if (subText != null) {
            TextView tvSub = new TextView(getContext());
            tvSub.setText(subText);
            tvSub.setTextColor(C_SUB);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            tvSub.setIncludeFontPadding(false);
            FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
            slp.bottomMargin = dp(GAP_V + 3);
            slp.leftMargin = dp(GAP_H + 5);
            keyLayout.addView(tvSub, slp);
        }

        if (hintText != null) {
            TextView tvHint = new TextView(getContext());
            tvHint.setText(hintText);
            tvHint.setTextColor(C_HINT);
            tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvHint.setIncludeFontPadding(false);
            FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
            hlp.topMargin = dp(GAP_V + 3);
            hlp.rightMargin = dp(GAP_H + 5);
            keyLayout.addView(tvHint, hlp);
        }

        keyLayout.setLayoutParams(keyParams(weight, heightDp));

        // Alternates shown uppercase while shifted (digits/symbols are unaffected)
        final List<String> shownAlts = new ArrayList<>();
        if (alts != null) {
            for (String a : alts) shownAlts.add(displayUpper ? a.toUpperCase() : a);
        }
        final boolean hasAlts = !shownAlts.isEmpty() && altListener != null;
        final boolean preview = !mainText.trim().isEmpty() && mainText.length() <= 3;

        keyLayout.setOnTouchListener(new OnTouchListener() {
            private boolean down = false;
            private boolean altActive = false;
            private Runnable longPress;

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!down) {
                            down = true;
                            keyPressed();
                        }
                        altActive = false;
                        keyLayout.setBackground(pressedBg);
                        feedback.key(Feedback.KeyType.LETTER);
                        if (preview) popups.showPreview(keyLayout, mainText);
                        startLongPress();
                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        // Another finger hit this key while the first is still on it (fast
                        // "ll" with two thumbs): the first touch counts as a finished tap,
                        // otherwise both presses would collapse into a single letter.
                        if (!altActive) {
                            cancelLongPress();
                            feedback.key(Feedback.KeyType.LETTER);
                            if (clickListener != null) clickListener.onClick(v);
                            startLongPress();
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
                            // Always commit: a finger that rolled off the key while typing fast
                            // still pressed it, and dropping the letter is far worse than
                            // typing the key the touch started on.
                            if (clickListener != null) clickListener.onClick(v);
                        }
                        release();
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        cancelLongPress();
                        keyLayout.setBackground(normalBg);
                        altActive = false;
                        popups.dismissAll();
                        release();
                        break;
                }
                return true;
            }

            private void startLongPress() {
                if (!hasAlts) return;
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

            private void cancelLongPress() {
                if (longPress != null) {
                    uiHandler.removeCallbacks(longPress);
                    longPress = null;
                }
            }

            private void release() {
                if (down) {
                    down = false;
                    keyReleased();
                }
            }
        });

        return keyLayout;
    }

    /** An emoji cell: no key plate, just the glyph, committed on tap. */
    private View createEmojiKey(final String emoji, float heightDp) {
        final TextView tv = new TextView(getContext());
        tv.setText(emoji);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        tv.setGravity(Gravity.CENTER);
        tv.setIncludeFontPadding(false);
        tv.setContentDescription(emoji);
        tv.setLayoutParams(keyParams(1f, heightDp));

        final Drawable pressedBg = plate(C_KEY);
        tv.setOnTouchListener(new OnTouchListener() {
            private boolean down = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!down) {
                            down = true;
                            keyPressed();
                        }
                        tv.setBackground(pressedBg);
                        feedback.key(Feedback.KeyType.LETTER);
                        break;
                    case MotionEvent.ACTION_UP:
                        tv.setBackground(null);
                        if (!isOutside(v, event)) typeText(emoji);
                        release();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        tv.setBackground(null);
                        release();
                        break;
                }
                return true;
            }

            private void release() {
                if (down) {
                    down = false;
                    keyReleased();
                }
            }
        });
        return tv;
    }

    /**
     * A function key showing either text or a vector icon. Fires on UP; an optional long press
     * runs instead of the click.
     */
    private View createFuncKey(String text, int iconRes, String description, float weight, int bgColor,
                               final Feedback.KeyType type, float heightDp, float textSp,
                               final OnClickListener clickListener, final Runnable onLongPress) {
        final FrameLayout key = new FrameLayout(getContext());
        key.setContentDescription(description);

        if (iconRes != 0) {
            key.addView(icon(iconRes, 24f));
        } else {
            TextView tv = new TextView(getContext());
            tv.setText(text);
            tv.setTextColor(C_TEXT);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp > 0 ? textSp : 15);
            tv.setGravity(Gravity.CENTER);
            tv.setSingleLine(true);
            tv.setIncludeFontPadding(false);
            key.addView(tv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        }

        final Drawable normalBg = plate(bgColor);
        final Drawable pressedBg = plate(lighten(bgColor, 0.35f));
        key.setBackground(normalBg);
        key.setLayoutParams(keyParams(weight, heightDp));

        key.setOnTouchListener(new OnTouchListener() {
            private boolean down = false;
            private boolean longFired = false;
            private Runnable longPress;

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!down) {
                            down = true;
                            keyPressed();
                        }
                        longFired = false;
                        key.setBackground(pressedBg);
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
                        key.setBackground(normalBg);
                        if (!longFired && !isOutside(v, event) && clickListener != null) clickListener.onClick(v);
                        release();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        cancelLongPress();
                        key.setBackground(normalBg);
                        release();
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

            private void release() {
                if (down) {
                    down = false;
                    keyReleased();
                }
            }
        });

        return key;
    }

    private View createBackspaceKey(float weight, float heightDp) {
        final FrameLayout key = new FrameLayout(getContext());
        key.setContentDescription("Delete");
        key.addView(icon(R.drawable.ic_backspace, 24f));

        final Drawable normalBg = plate(C_KEY_FUNC);
        final Drawable pressedBg = plate(lighten(C_KEY_FUNC, 0.35f));
        key.setBackground(normalBg);
        key.setLayoutParams(keyParams(weight, heightDp));

        key.setOnTouchListener(new OnTouchListener() {
            private boolean down = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!down) {
                            down = true;
                            keyPressed();
                        }
                        key.setBackground(pressedBg);
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
                        key.setBackground(normalBg);
                        stopRepeat();
                        if (down) {
                            down = false;
                            keyReleased();
                        }
                        break;
                }
                return true;
            }
        });

        return key;
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
        // Touches are cancelled on detach; make sure a deferred re-render is not stuck.
        keysDown = 0;
        if (renderPending) renderKeyboard();
    }

    // ---- drawing helpers ----

    private GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    /** Mixes a colour toward white; used for the pressed state of keys. */
    private static int lighten(int color, float amount) {
        int r = Color.red(color) + Math.round((255 - Color.red(color)) * amount * 0.4f);
        int g = Color.green(color) + Math.round((255 - Color.green(color)) * amount * 0.4f);
        int b = Color.blue(color) + Math.round((255 - Color.blue(color)) * amount * 0.4f);
        return Color.rgb(r, g, b);
    }

    private int dp(float dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }
}
