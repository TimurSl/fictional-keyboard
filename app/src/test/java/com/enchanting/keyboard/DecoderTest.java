package com.enchanting.keyboard;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DecoderTest {
    private static final Decoder.Script AUTO = Decoder.Script.AUTO;

    private static String roundTrip(String text, Translators.Mode mode, Decoder.Script script,
                                    Decoder.Target target) {
        return Decoder.decode(Translators.translateKey(text, mode), script, target);
    }

    @Test
    public void sgaRoundTripEnglish() {
        String s = "the quick brown fox jumps over a lazy dog";
        assertEquals(s, roundTrip(s, Translators.Mode.SGA, Decoder.Script.SGA, Decoder.Target.EN));
    }

    @Test
    public void sgaRoundTripRussian() {
        String s = "чего тебе надо";
        assertEquals(s, roundTrip(s, Translators.Mode.SGA, Decoder.Script.SGA, Decoder.Target.RU));
        assertEquals("чего тебе надо",
            Decoder.decode("ᓵ⍑ᒷ⊣𝙹 ℸ ̣ᒷʖᒷ リᔑ↸𝙹", AUTO, Decoder.Target.RU));
    }

    @Test
    public void sgaMultiGlyphLettersResolve() {
        assertEquals("я", Decoder.decode("||ᔑ", Decoder.Script.SGA, Decoder.Target.RU));
        assertEquals("ш", Decoder.decode("ᓭ⍑", Decoder.Script.SGA, Decoder.Target.RU));
        assertEquals("ч", Decoder.decode("ᓵ⍑", Decoder.Script.SGA, Decoder.Target.RU));
        assertEquals("ya", Decoder.decode("||ᔑ", Decoder.Script.SGA, Decoder.Target.EN));
        assertEquals("pet", Decoder.decode("!¡ᒷℸ ̣", Decoder.Script.SGA, Decoder.Target.EN));
    }

    @Test
    public void sgaYiHeuristic() {
        // мы -> ы after a consonant; май -> й after a vowel
        assertEquals("мы", roundTrip("мы", Translators.Mode.SGA, Decoder.Script.SGA, Decoder.Target.RU));
        assertEquals("май", roundTrip("май", Translators.Mode.SGA, Decoder.Script.SGA, Decoder.Target.RU));
    }

    @Test
    public void autoTargetPicksRussianOnCyrillicOnlySequences() {
        String sga = Translators.translateKey("чего тебе надо", Translators.Mode.SGA);
        assertEquals("чего тебе надо", Decoder.decode(sga, AUTO, Decoder.Target.AUTO));
    }

    @Test
    public void autoTargetKeepsEnglishWhenLatinOnlyGlyphPresent() {
        String sga = Translators.translateKey("she was", Translators.Mode.SGA);
        assertEquals("she was", Decoder.decode(sga, AUTO, Decoder.Target.AUTO));
    }

    @Test
    public void runesRoundTrip() {
        // letters that map one-to-one (no c/q/y/v/x collisions)
        String s = "the old bard sang of war";
        assertEquals(s, roundTrip(s, Translators.Mode.RUNES, Decoder.Script.RUNES, Decoder.Target.EN));
        String ru = "привет мир";
        assertEquals(ru, roundTrip(ru, Translators.Mode.RUNES, Decoder.Script.RUNES, Decoder.Target.RU));
    }

    @Test
    public void gothicRoundTrip() {
        String s = "hello world";
        assertEquals(s, roundTrip(s, Translators.Mode.GOTHIC, Decoder.Script.GOTHIC, Decoder.Target.EN));
        assertEquals(s, roundTrip(s, Translators.Mode.GOTHIC, AUTO, Decoder.Target.AUTO));
    }

    @Test
    public void zalgoIsStripped() {
        String z = Translators.translateKey("hello world", Translators.Mode.ZALGO);
        assertEquals("hello world", Decoder.decode(z, Decoder.Script.ZALGO, Decoder.Target.AUTO));
        assertEquals("hello world", Decoder.decode(z, AUTO, Decoder.Target.AUTO));
    }

    @Test
    public void detectsScripts() {
        assertEquals(Decoder.Script.SGA,
            Decoder.detect(Translators.translateKey("hello world", Translators.Mode.SGA)));
        assertEquals(Decoder.Script.RUNES,
            Decoder.detect(Translators.translateKey("hello world", Translators.Mode.RUNES)));
        assertEquals(Decoder.Script.GOTHIC,
            Decoder.detect(Translators.translateKey("hello world", Translators.Mode.GOTHIC)));
        assertEquals(Decoder.Script.ZALGO,
            Decoder.detect(Translators.translateKey("hello world", Translators.Mode.ZALGO)));
        assertEquals(Decoder.Script.OZKAVOSH, Decoder.detect("ozh icha tak izh"));
        assertEquals(Decoder.Script.NONE, Decoder.detect("just some plain text"));
        assertEquals(Decoder.Script.NONE, Decoder.detect(""));
    }

    @Test
    public void plainTextPassesThroughOnAuto() {
        assertEquals("plain text", Decoder.decode("plain text", AUTO, Decoder.Target.AUTO));
    }

    @Test
    public void ozkavoshAndArirallDecode() {
        assertEquals("i will take you", Decoder.decode("ozh icha tak izh", AUTO, Decoder.Target.AUTO));
        assertEquals("apple", Decoder.decode("appule", Decoder.Script.ARIRAL, Decoder.Target.AUTO));
    }
}
