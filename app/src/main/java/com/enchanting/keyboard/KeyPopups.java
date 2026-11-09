package com.enchanting.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Key preview bubble and the long-press alternates strip. Both are non-touchable popups. */
public final class KeyPopups {

    private final View root;
    private final Context context;

    private PopupWindow previewWindow;
    private PopupWindow altWindow;
    private final List<TextView> altViews = new ArrayList<>();
    private List<String> alts = new ArrayList<>();
    private int selected = -1;

    public KeyPopups(View root) {
        this.root = root;
        this.context = root.getContext();
    }

    // ---- Preview bubble ----

    public void showPreview(View key, String text) {
        hidePreview();
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, text.length() > 2 ? 22 : 30);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(rounded("#4A4846", 12));

        int w = Math.max(dp(52), key.getWidth() + dp(12));
        int h = dp(62);
        previewWindow = newPopup(tv, w, h);

        int[] loc = new int[2];
        key.getLocationInWindow(loc);
        int x = loc[0] + key.getWidth() / 2 - w / 2;
        int y = loc[1] - h - dp(2);
        show(previewWindow, key, x, y);
    }

    public void hidePreview() {
        if (previewWindow != null) {
            try { previewWindow.dismiss(); } catch (RuntimeException ignored) { }
            previewWindow = null;
        }
    }

    // ---- Alternates strip ----

    public boolean isAlternatesOpen() {
        return altWindow != null;
    }

    public void showAlternates(View key, List<String> choices) {
        dismissAlternates();
        if (choices.isEmpty()) return;
        alts = new ArrayList<>(choices);

        LinearLayout strip = new LinearLayout(context);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER);
        strip.setPadding(dp(4), dp(4), dp(4), dp(4));
        strip.setBackground(rounded("#2F2E2C", 14));

        int cell = dp(40);
        for (String a : alts) {
            TextView tv = new TextView(context);
            tv.setText(a);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(cell, dp(48)));
            strip.addView(tv);
            altViews.add(tv);
        }

        int w = cell * alts.size() + dp(8);
        int h = dp(56);
        altWindow = newPopup(strip, w, h);

        int[] loc = new int[2];
        key.getLocationInWindow(loc);
        int rootW = root.getWidth() > 0 ? root.getWidth() : context.getResources().getDisplayMetrics().widthPixels;
        int x = loc[0] + key.getWidth() / 2 - w / 2;
        x = Math.max(0, Math.min(rootW - w, x));
        int y = loc[1] - h - dp(6);
        show(altWindow, key, x, y);
        select(0);
    }

    /** Highlights the choice under the given screen x coordinate; returns its index. */
    public int highlightAt(float rawX) {
        if (altViews.isEmpty()) return -1;
        int idx = altViews.size() - 1;
        int[] loc = new int[2];
        for (int i = 0; i < altViews.size(); i++) {
            View v = altViews.get(i);
            v.getLocationOnScreen(loc);
            if (rawX < loc[0] + v.getWidth()) {
                idx = i;
                break;
            }
        }
        select(idx);
        return idx;
    }

    public void dismissAlternates() {
        if (altWindow != null) {
            try { altWindow.dismiss(); } catch (RuntimeException ignored) { }
            altWindow = null;
        }
        altViews.clear();
        alts = new ArrayList<>();
        selected = -1;
    }

    public void dismissAll() {
        hidePreview();
        dismissAlternates();
    }

    private void select(int idx) {
        if (idx == selected) return;
        selected = idx;
        for (int i = 0; i < altViews.size(); i++) {
            altViews.get(i).setBackground(i == idx ? rounded("#3E91FF", 10) : null);
        }
    }

    // ---- helpers ----

    private PopupWindow newPopup(View content, int w, int h) {
        PopupWindow pw = new PopupWindow(content, w, h);
        pw.setTouchable(false);
        pw.setFocusable(false);
        pw.setClippingEnabled(false);
        pw.setAnimationStyle(0);
        return pw;
    }

    private void show(PopupWindow pw, View anchor, int x, int y) {
        try {
            pw.showAtLocation(root, Gravity.NO_GRAVITY, x, y);
        } catch (RuntimeException e) {
            // window token may be gone while the keyboard is closing
        }
    }

    private GradientDrawable rounded(String color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.parseColor(color));
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
            context.getResources().getDisplayMetrics());
    }
}
