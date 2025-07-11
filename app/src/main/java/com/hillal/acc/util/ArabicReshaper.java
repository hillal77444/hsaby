package com.hillal.acc.util;

public class ArabicReshaper {
    public static String reshape(String input) {
        return BetterArabicReshaper.reshapeAndBidi(input);
    }
} 