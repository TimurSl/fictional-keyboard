package com.enchanting.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TranslatorsTest {
    @Test
    public void sgaEncodesLatinAndCyrillic() {
        assertEquals("ᔑ", Translators.translateKey("a", Translators.Mode.SGA));
        assertEquals("ᓵ⍑ᒷ⊣𝙹 ℸ ̣ᒷʖᒷ リᔑ↸𝙹",
            Translators.translateKey("чего тебе надо", Translators.Mode.SGA));
    }

    @Test
    public void normalModeIsIdentity() {
        assertEquals("hello", Translators.translateKey("hello", Translators.Mode.NORMAL));
    }

    @Test
    public void zalgoAddsCombiningMarksAndKeepsLetters() {
        String z = Translators.translateKey("abc", Translators.Mode.ZALGO);
        assertTrue(z.length() > 3);
        assertEquals("abc", Decoder.stripCombining(z));
    }

    @Test
    public void wordModesRouteToGameTranslators() {
        assertEquals("ozh icha tak izh",
            Translators.translateKey("I will have you", Translators.Mode.DEMON));
        assertEquals("appule", Translators.translateKey("apple", Translators.Mode.ARIRAL));
        assertTrue(Translators.Mode.DEMON.isWordLevel());
        assertTrue(!Translators.Mode.SGA.isWordLevel());
    }
}
