package com.hillal.acc.util;

import java.text.Bidi;
import java.util.HashMap;
import java.util.Map;

public class BetterArabicReshaper {
    // --- بداية كود Better-Arabic-Reshaper (Java port) ---
    // المصدر: https://github.com/AhmedEssam/Better-Arabic-Reshaper
    // تم التبسيط والدمج مع المشروع الحالي

    // Presentation Forms-B Unicode
    private static final char[] ARABIC_GLYPHS = {
        '\uFE80', '\uFE81', '\uFE82', '\uFE83', '\uFE84', '\uFE85', '\uFE86', '\uFE87', '\uFE88',
        '\uFE89', '\uFE8A', '\uFE8B', '\uFE8C', '\uFE8D', '\uFE8E', '\uFE8F', '\uFE90', '\uFE91',
        '\uFE92', '\uFE93', '\uFE94', '\uFE95', '\uFE96', '\uFE97', '\uFE98', '\uFE99', '\uFE9A',
        '\uFE9B', '\uFE9C', '\uFE9D', '\uFE9E', '\uFE9F', '\uFEA0', '\uFEA1', '\uFEA2', '\uFEA3',
        '\uFEA4', '\uFEA5', '\uFEA6', '\uFEA7', '\uFEA8', '\uFEA9', '\uFEAA', '\uFEAB', '\uFEAC',
        '\uFEAD', '\uFEAE', '\uFEAF', '\uFEB0', '\uFEB1', '\uFEB2', '\uFEB3', '\uFEB4', '\uFEB5',
        '\uFEB6', '\uFEB7', '\uFEB8', '\uFEB9', '\uFEBA', '\uFEBB', '\uFEBC', '\uFEBD', '\uFEBE',
        '\uFEBF', '\uFEC0', '\uFEC1', '\uFEC2', '\uFEC3', '\uFEC4', '\uFEC5', '\uFEC6', '\uFEC7',
        '\uFEC8', '\uFEC9', '\uFECA', '\uFECB', '\uFECC', '\uFECD', '\uFECE', '\uFECF', '\uFED0',
        '\uFED1', '\uFED2', '\uFED3', '\uFED4', '\uFED5', '\uFED6', '\uFED7', '\uFED8', '\uFED9',
        '\uFEDA', '\uFEDB', '\uFEDC', '\uFEDD', '\uFEDE', '\uFEDF', '\uFEE0', '\uFEE1', '\uFEE2',
        '\uFEE3', '\uFEE4', '\uFEE5', '\uFEE6', '\uFEE7', '\uFEE8', '\uFEE9', '\uFEEA', '\uFEEB',
        '\uFEEC', '\uFEED', '\uFEEE', '\uFEEF', '\uFEF0', '\uFEF1', '\uFEF2', '\uFEF3', '\uFEF4',
        '\uFEF5', '\uFEF6', '\uFEF7', '\uFEF8', '\uFEF9', '\uFEFA', '\uFEFB', '\uFEFC'
    };

    // Arabic letters and their forms
    private static final Map<Character, GlyphForms> ARABIC_FORMS_MAP = new HashMap<>();
    static {
        // فقط جزء من الحروف هنا كمثال، يجب نقل كل الحروف من كود المكتبة الأصلية
        ARABIC_FORMS_MAP.put('\u0627', new GlyphForms('\uFE8D', '\uFE8E', '\uFE8D', '\uFE8D')); // Alef
        ARABIC_FORMS_MAP.put('\u0628', new GlyphForms('\uFE8F', '\uFE90', '\uFE91', '\uFE92')); // Beh
        ARABIC_FORMS_MAP.put('\u062A', new GlyphForms('\uFE95', '\uFE96', '\uFE97', '\uFE98')); // Teh
        ARABIC_FORMS_MAP.put('\u062B', new GlyphForms('\uFE99', '\uFE9A', '\uFE9B', '\uFE9C')); // Theh
        ARABIC_FORMS_MAP.put('\u062C', new GlyphForms('\uFE9D', '\uFE9E', '\uFE9F', '\uFEA0')); // Jeem
        ARABIC_FORMS_MAP.put('\u062D', new GlyphForms('\uFEA1', '\uFEA2', '\uFEA3', '\uFEA4')); // Hah
        ARABIC_FORMS_MAP.put('\u062E', new GlyphForms('\uFEA5', '\uFEA6', '\uFEA7', '\uFEA8')); // Khah
        ARABIC_FORMS_MAP.put('\u0644', new GlyphForms('\uFEDD', '\uFEDE', '\uFEDF', '\uFEE0')); // Lam
        ARABIC_FORMS_MAP.put('\u0645', new GlyphForms('\uFEE1', '\uFEE2', '\uFEE3', '\uFEE4')); // Meem
        ARABIC_FORMS_MAP.put('\u0646', new GlyphForms('\uFEE5', '\uFEE6', '\uFEE7', '\uFEE8')); // Noon
        ARABIC_FORMS_MAP.put('\u0647', new GlyphForms('\uFEE9', '\uFEEA', '\uFEEB', '\uFEEC')); // Heh
        ARABIC_FORMS_MAP.put('\u0648', new GlyphForms('\uFEED', '\uFEEE', '\uFEED', '\uFEED')); // Waw
        ARABIC_FORMS_MAP.put('\u064A', new GlyphForms('\uFEF1', '\uFEF2', '\uFEF3', '\uFEF4')); // Yeh
        // ... أضف باقي الحروف من المكتبة الأصلية هنا ...
    }

    private static class GlyphForms {
        char isolated, finalForm, initial, medial;
        GlyphForms(char isolated, char finalForm, char initial, char medial) {
            this.isolated = isolated;
            this.finalForm = finalForm;
            this.initial = initial;
            this.medial = medial;
        }
    }

    private static boolean isArabicChar(char c) {
        return ARABIC_FORMS_MAP.containsKey(c);
    }

    private static char getForm(char c, int form) {
        GlyphForms forms = ARABIC_FORMS_MAP.get(c);
        if (forms == null) return c;
        switch (form) {
            case 0: return forms.isolated;
            case 1: return forms.finalForm;
            case 2: return forms.initial;
            case 3: return forms.medial;
            default: return c;
        }
    }

    // --- نهاية كود Better-Arabic-Reshaper (Java port) ---

    /**
     * تعيد النص العربي بشكل صحيح (تشكيل احترافي)
     */
    public static String reshapeArabic(String text) {
        if (text == null) return "";
        char[] chars = text.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            char curr = chars[i];
            if (!isArabicChar(curr)) {
                sb.append(curr);
                continue;
            }
            boolean prevArabic = (i > 0) && isArabicChar(chars[i - 1]);
            boolean nextArabic = (i < chars.length - 1) && isArabicChar(chars[i + 1]);
            if (!prevArabic && !nextArabic) {
                sb.append(getForm(curr, 0)); // isolated
            } else if (!prevArabic && nextArabic) {
                sb.append(getForm(curr, 2)); // initial
            } else if (prevArabic && !nextArabic) {
                sb.append(getForm(curr, 1)); // final
            } else {
                sb.append(getForm(curr, 3)); // medial
            }
        }
        return sb.toString();
    }

    /**
     * تعيد النص العربي بشكل صحيح للعرض: تشكل الحروف وتضبط اتجاه النص (RTL)
     * استخدم هذه الدالة بدلاً من reshapeArabic فقط عند الطباعة أو التصدير.
     * @param text النص العربي الأصلي
     * @return نص عربي مشكل وجاهز للعرض من اليمين لليسار
     */
    public static String reshapeAndBidi(String text) {
        String reshaped = reshapeArabic(text);
        String[] lines = reshaped.split("\\r?\\n");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            result.append(reverseArabicOnly(lines[i]));
            if (i < lines.length - 1) result.append("\n");
        }
        return result.toString();
    }

    private static boolean isNumberOrSeparator(char c) {
        // الأرقام العربية والهندية والفواصل والنقاط
        return (c >= '0' && c <= '9') ||
               (c >= '\u0660' && c <= '\u0669') || // أرقام هندية
               c == ',' || c == '.' || c == '٫' || c == '٬' || c == '،' || c == '%';
    }

    /**
     * تعكس فقط الحروف العربية في السطر وتترك الأرقام والإنجليزي في مكانهم
     */
    private static String reverseArabicOnly(String line) {
        StringBuilder sb = new StringBuilder();
        int i = line.length() - 1;
        while (i >= 0) {
            char c = line.charAt(i);
            if (isArabicChar(c)) {
                // اجمع كل الحروف العربية المتتالية
                int arabicEnd = i;
                while (i >= 0 && isArabicChar(line.charAt(i))) i--;
                sb.append(line.substring(i + 1, arabicEnd + 1));
            } else if (isNumberOrSeparator(c)) {
                // اجمع كل الأرقام والفواصل المتتالية
                int numEnd = i;
                while (i >= 0 && isNumberOrSeparator(line.charAt(i))) i--;
                sb.append(line.substring(i + 1, numEnd + 1));
            } else {
                sb.append(c);
                i--;
            }
        }
        return sb.toString();
    }
} 