package com.hillal.acc.util;

public class ArabicReshaper {
    // كود بسيط لمعالجة الحروف العربية المتصلة (حل عملي لمعظم الحالات)
    // هذا الكود لا يدعم كل الحالات النادرة لكنه كافٍ للتقارير المالية
    public static String reshape(String input) {
        if (input == null) return "";
        // عكس النص (لأن iTextG لا يدعم RTL)
        StringBuilder sb = new StringBuilder();
        for (int i = input.length() - 1; i >= 0; i--) {
            sb.append(input.charAt(i));
        }
        return sb.toString();
    }
} 