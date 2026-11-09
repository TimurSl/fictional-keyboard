package com.enchanting.keyboard;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

public class SgaInputMethodService extends InputMethodService implements SgaKeyboardView.OnKeyActionListener {

    private SgaKeyboardView keyboardView;
    private long lastComposeMs;

    @Override
    public View onCreateInputView() {
        keyboardView = new SgaKeyboardView(this, this);
        return keyboardView;
    }

    @Override
    public void onFinishInput() {
        // Commit a half-typed Ozkavosh/Ariral word before the connection goes away.
        if (keyboardView != null) keyboardView.flushComposing();
        super.onFinishInput();
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        // The user moved the cursor away and the editor dropped our composing region:
        // forget the buffer so later typing does not re-insert the old raw text.
        if (keyboardView != null && keyboardView.hasComposing() && candidatesStart < 0
                && SystemClock.uptimeMillis() - lastComposeMs > 400) {
            keyboardView.clearComposing();
        }
    }

    @Override
    public void onComposing(String raw) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        lastComposeMs = SystemClock.uptimeMillis();
        ic.setComposingText(raw, 1);
        if (raw.isEmpty()) ic.finishComposingText();
    }

    @Override
    public void onCommitWord(String translated) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        lastComposeMs = SystemClock.uptimeMillis();
        ic.commitText(translated, 1);
    }

    @Override
    public String getSelectedText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return null;
        CharSequence cs = ic.getSelectedText(0);
        return cs != null ? cs.toString() : null;
    }

    @Override
    public String getTextBeforeCursor(int n) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return null;
        CharSequence cs = ic.getTextBeforeCursor(n, 0);
        return cs != null ? cs.toString() : null;
    }

    @Override
    public void onShowImePicker() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showInputMethodPicker();
    }

    @Override
    public void onText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && text != null) {
            ic.commitText(text, 1);
        }
    }

    @Override
    public void onDelete() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        CharSequence before = ic.getTextBeforeCursor(4, 0);
        if (before == null || before.length() == 0) {
            ic.deleteSurroundingText(1, 0);
            return;
        }

        int len = before.length();
        char last = before.charAt(len - 1);

        // Check if low surrogate of a surrogate pair (e.g. mathematical bold italic '𝙹')
        if (Character.isLowSurrogate(last) && len >= 2 && Character.isHighSurrogate(before.charAt(len - 2))) {
            ic.deleteSurroundingText(2, 0);
        } else if (last == '\u0323' || (last >= 0x0300 && last <= 0x036F)) {
            // Combining diacritical mark (like ℸ ̣ or Zalgo marks)
            int toDelete = 2;
            if (len >= 3 && Character.isLowSurrogate(before.charAt(len - 2))) {
                toDelete = 3;
            }
            ic.deleteSurroundingText(toDelete, 0);
        } else {
            ic.deleteSurroundingText(1, 0);
        }
    }

    @Override
    public void onEnter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        EditorInfo info = getCurrentInputEditorInfo();
        if (info != null) {
            int action = info.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);
            switch (action) {
                case EditorInfo.IME_ACTION_GO:
                case EditorInfo.IME_ACTION_NEXT:
                case EditorInfo.IME_ACTION_SEARCH:
                case EditorInfo.IME_ACTION_SEND:
                case EditorInfo.IME_ACTION_DONE:
                    ic.performEditorAction(action);
                    return;
            }
        }
        ic.commitText("\n", 1);
    }

    @Override
    public void onSwitchIme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (switchToNextInputMethod(false)) return;
            } catch (Exception ignored) {}
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showInputMethodPicker();
        }
    }
}
