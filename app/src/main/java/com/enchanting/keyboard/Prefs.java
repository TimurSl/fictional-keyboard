package com.enchanting.keyboard;

import android.content.Context;
import android.content.SharedPreferences;

/** Keyboard settings shared by the IME and the settings activity (same process, same file). */
public final class Prefs {
    private static final String FILE = "sga_keyboard";

    public static final String HAPTICS_ON = "haptics_on";
    public static final String HAPTIC_STRENGTH = "haptic_strength";
    public static final String SOUND_ON = "sound_on";
    public static final String SOUND_VOLUME = "sound_volume";
    public static final String DECODE_TARGET = "decode_target";
    public static final String LAST_MODE = "last_mode";
    public static final String LAST_LAYOUT = "last_layout";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean hapticsOn() { return sp.getBoolean(HAPTICS_ON, true); }
    public void setHapticsOn(boolean v) { sp.edit().putBoolean(HAPTICS_ON, v).apply(); }

    /** 0..100 */
    public int hapticStrength() { return sp.getInt(HAPTIC_STRENGTH, 60); }
    public void setHapticStrength(int v) { sp.edit().putInt(HAPTIC_STRENGTH, clamp(v)).apply(); }

    public boolean soundOn() { return sp.getBoolean(SOUND_ON, false); }
    public void setSoundOn(boolean v) { sp.edit().putBoolean(SOUND_ON, v).apply(); }

    /** 0..100 */
    public int soundVolume() { return sp.getInt(SOUND_VOLUME, 50); }
    public void setSoundVolume(int v) { sp.edit().putInt(SOUND_VOLUME, clamp(v)).apply(); }

    public Decoder.Target decodeTarget() {
        return Decoder.Target.fromName(sp.getString(DECODE_TARGET, Decoder.Target.AUTO.name()));
    }
    public void setDecodeTarget(Decoder.Target t) { sp.edit().putString(DECODE_TARGET, t.name()).apply(); }

    public String lastMode() { return sp.getString(LAST_MODE, null); }
    public void setLastMode(String v) { sp.edit().putString(LAST_MODE, v).apply(); }

    public String lastLayout() { return sp.getString(LAST_LAYOUT, null); }
    public void setLastLayout(String v) { sp.edit().putString(LAST_LAYOUT, v).apply(); }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
