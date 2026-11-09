package com.enchanting.keyboard;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Key haptics and click sounds. Fire on ACTION_DOWN for an immediate, Samsung-like feel.
 */
public final class Feedback {

    public enum KeyType {
        LETTER, SPACE, ENTER, DELETE, DELETE_REPEAT, SPECIAL, POPUP_OPEN
    }

    private final View owner;
    private final Prefs prefs;
    private final Vibrator vibrator;
    private final AudioManager audio;

    public Feedback(View owner, Prefs prefs) {
        this.owner = owner;
        this.prefs = prefs;
        Context ctx = owner.getContext();
        Vibrator v = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) v = vm.getDefaultVibrator();
            } else {
                v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            }
        } catch (RuntimeException ignored) {
            // no vibrator service
        }
        vibrator = v;
        audio = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
    }

    public void key(KeyType type) {
        if (prefs.hapticsOn()) haptic(type);
        if (prefs.soundOn()) sound(type);
    }

    private void haptic(KeyType type) {
        int strength = prefs.hapticStrength();
        if (strength <= 0) return;

        if (vibrator == null || !vibrator.hasVibrator()) {
            // Fall back to the view's haptics, ignoring the system "touch feedback" switch
            // because the user explicitly enabled haptics in our own settings.
            owner.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(predefinedFor(type, strength)));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int ms = durationFor(type, strength);
                int amplitude = Math.max(1, Math.min(255, 40 + strength * 2));
                vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude));
            } else {
                vibrator.vibrate(durationFor(type, strength));
            }
        } catch (RuntimeException ignored) {
            // vibration is best-effort
        }
    }

    /** Predefined effects have a fixed intensity, so strength selects how crisp the effect is. */
    private static int predefinedFor(KeyType type, int strength) {
        switch (type) {
            case POPUP_OPEN:
                return VibrationEffect.EFFECT_HEAVY_CLICK;
            case SPACE:
            case ENTER:
                return strength < 34 ? VibrationEffect.EFFECT_TICK : VibrationEffect.EFFECT_CLICK;
            case DELETE_REPEAT:
                return VibrationEffect.EFFECT_TICK;
            case DELETE:
            case SPECIAL:
            case LETTER:
            default:
                return strength < 80 ? VibrationEffect.EFFECT_TICK : VibrationEffect.EFFECT_CLICK;
        }
    }

    /** Pre-Q: map 0..100 strength to 8..25 ms, shorter for light keys, longer for heavy ones. */
    private static int durationFor(KeyType type, int strength) {
        int base = 8 + (strength * 17) / 100;
        switch (type) {
            case POPUP_OPEN: return Math.min(40, base + 8);
            case SPACE:
            case ENTER: return Math.min(30, base + 3);
            case DELETE_REPEAT: return Math.max(6, base - 4);
            default: return base;
        }
    }

    private void sound(KeyType type) {
        if (audio == null || type == KeyType.POPUP_OPEN || type == KeyType.DELETE_REPEAT) return;
        int fx;
        switch (type) {
            case SPACE: fx = AudioManager.FX_KEYPRESS_SPACEBAR; break;
            case DELETE: fx = AudioManager.FX_KEYPRESS_DELETE; break;
            case ENTER: fx = AudioManager.FX_KEYPRESS_RETURN; break;
            default: fx = AudioManager.FX_KEYPRESS_STANDARD; break;
        }
        audio.playSoundEffect(fx, prefs.soundVolume() / 100f);
    }
}
