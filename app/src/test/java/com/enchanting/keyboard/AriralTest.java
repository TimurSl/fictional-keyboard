package com.enchanting.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AriralTest {
    @Test
    public void wikiExample() {
        assertEquals("appule", Ariral.translateWord("apple"));
        assertEquals("Appule", Ariral.translateWord("Apple"));
    }

    @Test
    public void consonantEndingsGetA() {
        assertEquals("doga", Ariral.translateWord("dog"));
        assertEquals("maka", Ariral.translateWord("make"));
    }

    @Test
    public void vowelEndingsStay() {
        assertEquals("hello", Ariral.translateWord("hello"));
        assertEquals("the", Ariral.translateWord("the"));
    }

    @Test
    public void rBecomesDoubleR() {
        assertTrue(Ariral.translateWord("red").startsWith("rr"));
        assertEquals("rreda", Ariral.translateWord("red"));
        assertEquals("carra", Ariral.translateWord("car"));
    }

    @Test
    public void silentLettersDropped() {
        assertEquals("nita", Ariral.translateWord("knit"));
        assertEquals("rrota", Ariral.translateWord("wrote"));
        assertEquals("nita", Ariral.translateWord("night"));
    }

    @Test
    public void clustersGetU() {
        assertEquals("hanuda", Ariral.translateWord("hand"));
    }

    @Test
    public void russianIsTransliterated() {
        assertEquals("doma", Ariral.translateWord("дом"));
    }

    @Test
    public void specialWordsAndNumbers() {
        assertEquals("ack", Ariral.translateWord("lol"));
        assertEquals("ack", Ariral.translateWord("haha"));
        assertEquals("ack", Ariral.translateWord("хаха"));
        assertEquals("doga 42 doga", Ariral.translate("dog 42 dog"));
        assertEquals("42", Ariral.translate("42"));
    }

    @Test
    public void reverseHeuristics() {
        assertEquals("dog", Ariral.decodeWord("doga"));
        assertEquals("apple", Ariral.decodeWord("appule"));
        assertEquals("red", Ariral.decodeWord("rreda"));
        assertEquals("hello", Ariral.decodeWord("hello"));
        assertEquals("lol", Ariral.decodeWord("ack"));
    }

    @Test
    public void coinedNumberWords() {
        assertEquals("una duva tarra korra", Ariral.translate("one two three four"));
        assertEquals("Duva", Ariral.translateWord("Two"));
        assertEquals("pima sika", Ariral.translate("пять шесть"));
        assertEquals("ula", Ariral.translateWord("zero"));
        // digits are still left alone
        assertEquals("42", Ariral.translate("42"));
    }

    @Test
    public void coinedNumbersDecodeBack() {
        assertEquals("three", Ariral.decodeWord("tarra"));
        assertEquals("ten", Ariral.decodeWord("deka"));
        assertEquals("seven", Ariral.decodeWord("seva"));
    }
}
