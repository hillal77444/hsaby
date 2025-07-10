package com.hillal.acc.util;

import java.text.Bidi;

public class BetterArabicReshaper {
    // كود ربط الحروف العربية (مقتبس من Better-Arabic-Reshaper - نسخة مبسطة)
    // يدعم معظم الحروف العربية الشائعة ويكفي للتقارير المالية
    // ملاحظة: هذا الكود لا يدعم التشكيل أو كل الرموز النادرة، لكنه كافٍ للنصوص المالية

    // Presentation Forms-B Unicode ranges
    private static final char[][] ARABIC_FORMS = {
        // {isolated, final, initial, medial}
        {'\u0627', '\uFE8E', '\uFE8D', '\uFE8D', '\uFE8D'}, // Alef
        {'\u0628', '\uFE90', '\uFE91', '\uFE92', '\uFE91'}, // Beh
        {'\u062A', '\uFE96', '\uFE97', '\uFE98', '\uFE97'}, // Teh
        {'\u062B', '\uFE9A', '\uFE9B', '\uFE9C', '\uFE9B'}, // Theh
        {'\u062C', '\uFE9E', '\uFE9F', '\uFEA0', '\uFE9F'}, // Jeem
        {'\u062D', '\uFEA2', '\uFEA3', '\uFEA4', '\uFEA3'}, // Hah
        {'\u062E', '\uFEA6', '\uFEA7', '\uFEA8', '\uFEA7'}, // Khah
        {'\u062F', '\uFEAA', '\uFEA9', '\uFEA9', '\uFEA9'}, // Dal
        {'\u0630', '\uFEAC', '\uFEAB', '\uFEAB', '\uFEAB'}, // Thal
        {'\u0631', '\uFEAE', '\uFEAD', '\uFEAD', '\uFEAD'}, // Reh
        {'\u0632', '\uFEB0', '\uFEAF', '\uFEAF', '\uFEAF'}, // Zain
        {'\u0633', '\uFEB2', '\uFEB3', '\uFEB4', '\uFEB3'}, // Seen
        {'\u0634', '\uFEB6', '\uFEB7', '\uFEB8', '\uFEB7'}, // Sheen
        {'\u0635', '\uFEBA', '\uFEBB', '\uFEBC', '\uFEBB'}, // Sad
        {'\u0636', '\uFEBE', '\uFEBF', '\uFEC0', '\uFEBF'}, // Dad
        {'\u0637', '\uFEC2', '\uFEC3', '\uFEC4', '\uFEC3'}, // Tah
        {'\u0638', '\uFEC6', '\uFEC7', '\uFEC8', '\uFEC7'}, // Zah
        {'\u0639', '\uFECA', '\uFECB', '\uFECC', '\uFECB'}, // Ain
        {'\u063A', '\uFECE', '\uFECF', '\uFED0', '\uFECF'}, // Ghain
        {'\u0641', '\uFED2', '\uFED3', '\uFED4', '\uFED3'}, // Feh
        {'\u0642', '\uFED6', '\uFED7', '\uFED8', '\uFED7'}, // Qaf
        {'\u0643', '\uFEDA', '\uFEDB', '\uFEDC', '\uFEDB'}, // Kaf
        {'\u0644', '\uFEDE', '\uFEDF', '\uFEE0', '\uFEDF'}, // Lam
        {'\u0645', '\uFEE2', '\uFEE3', '\uFEE4', '\uFEE3'}, // Meem
        {'\u0646', '\uFEE6', '\uFEE7', '\uFEE8', '\uFEE7'}, // Noon
        {'\u0647', '\uFEEA', '\uFEEB', '\uFEEC', '\uFEEB'}, // Heh
        {'\u0648', '\uFEEE', '\uFEED', '\uFEED', '\uFEED'}, // Waw
        {'\u064A', '\uFEF2', '\uFEF3', '\uFEF4', '\uFEF3'}, // Yeh
        {'\u0649', '\uFEEF', '\uFBE8', '\uFBE8', '\uFBE8'}, // Alef Maqsura
        {'\u0623', '\uFE84', '\uFE83', '\uFE83', '\uFE83'}, // Alef with Hamza Above
        {'\u0625', '\uFE88', '\uFE87', '\uFE87', '\uFE87'}, // Alef with Hamza Below
        {'\u0622', '\uFE82', '\uFE81', '\uFE81', '\uFE81'}, // Alef Madda
        {'\u0629', '\uFE94', '\uFE93', '\uFE93', '\uFE93'}, // Teh Marbuta
        {'\u0640', '\u0640', '\u0640', '\u0640', '\u0640'}  // Tatweel
    };

    private static boolean isArabicChar(char c) {
        for (char[] form : ARABIC_FORMS) {
            if (form[0] == c) return true;
        }
        return false;
    }

    private static char getForm(char c, int form) {
        for (char[] forms : ARABIC_FORMS) {
            if (forms[0] == c) return forms[form];
        }
        return c;
    }

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
                sb.append(getForm(curr, 1)); // isolated
            } else if (!prevArabic && nextArabic) {
                sb.append(getForm(curr, 2)); // initial
            } else if (prevArabic && !nextArabic) {
                sb.append(getForm(curr, 1)); // final
            } else {
                sb.append(getForm(curr, 3)); // medial
            }
        }
        // معالجة اتجاه النص (Bidi)
        Bidi bidi = new Bidi(sb.toString(), Bidi.DIRECTION_RIGHT_TO_LEFT);
        return bidi.writeReordered(Bidi.DO_MIRRORING);
    }
} 