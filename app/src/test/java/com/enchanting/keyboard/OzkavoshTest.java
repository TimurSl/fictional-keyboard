package com.enchanting.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OzkavoshTest {
    @Test
    public void basicSentence() {
        assertEquals("ozh icha tak izh", Ozkavosh.translate("I will have you"));
    }

    @Test
    public void notPrefixAttachesToNextWord() {
        assertEquals("vo'nesh", Ozkavosh.translate("not safe"));
        assertEquals("vo'nesh", Ozkavosh.translate("не безопасный"));
    }

    @Test
    public void veryPrefixAndComparatives() {
        assertEquals("ahm'voth", Ozkavosh.translate("very war"));
        assertEquals("ashm", Ozkavosh.translate("more"));
        assertEquals("vohm", Ozkavosh.translate("меньше"));
    }

    @Test
    public void russianEquivalents() {
        assertEquals("ozh", Ozkavosh.translate("я"));
        assertEquals("ozh", Ozkavosh.translate("меня"));
        assertEquals("omoz", Ozkavosh.translate("тьма"));
        assertEquals("voth", Ozkavosh.translate("война"));
        assertEquals("voth", Ozkavosh.translate("войны"));
    }

    @Test
    public void stemming() {
        assertEquals("sek", Ozkavosh.translate("comes"));
        assertEquals("gluth", Ozkavosh.translate("eating"));
        assertEquals("domosh", Ozkavosh.translate("reigning"));
        assertEquals("fek", Ozkavosh.translate("stopped"));
    }

    @Test
    public void punctuationAndCaseArePreserved() {
        assertEquals("ozh domosh, izh!", Ozkavosh.translate("My REIGN, you!"));
    }

    @Test
    public void unknownWordsAreDeterministic() {
        String a = Ozkavosh.translate("banana");
        assertEquals(a, Ozkavosh.translate("banana"));
        assertEquals("zovo", Ozkavosh.demonize("zoovo"));
        assertEquals("kat", Ozkavosh.demonize("cat"));
        assertEquals("dogzh", Ozkavosh.demonize("dogs"));
        assertEquals("brot", Ozkavosh.demonize("breet"));
    }

    @Test
    public void reverse() {
        assertEquals("не дом", Ozkavosh.decode("vo'nesh", true));
        assertEquals("not home", Ozkavosh.decode("vo'nesh", false));
        assertEquals("i will take you", Ozkavosh.decode("ozh icha tak izh", false));
        assertEquals("very war", Ozkavosh.decode("ahm'voth", false));
        assertEquals("я", Ozkavosh.decode("ozh", true));
    }

    @Test
    public void modifierDetection() {
        assertEquals("vo'", Ozkavosh.modifierPrefix("Not"));
        assertEquals("ahm'", Ozkavosh.modifierPrefix("очень"));
        assertNull(Ozkavosh.modifierPrefix("safe"));
    }
}
