package com.hillal.acc.util;

public class ArabicReshaper {
    // تعكس الحروف داخل كل كلمة ثم تعكس ترتيب الكلمات
    public static String reshape(String input) {
        if (input == null) return "";
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(new StringBuilder(words[i]).reverse());
            if (i != 0) sb.append(" ");
        }
        return sb.toString();
    }
} 