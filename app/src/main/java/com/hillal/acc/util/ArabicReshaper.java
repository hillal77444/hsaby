package com.hillal.acc.util;

public class ArabicReshaper {
    // كود ربط الحروف العربية الأساسي (shaping) مع عكس النص لعرضه بشكل متصل في PDF
    // هذا الكود يدعم معظم الحروف العربية الشائعة ويكفي للتقارير المالية
    private static final char[] ISOLATED = {
        '\u0627','\u0628','\u062A','\u062B','\u062C','\u062D','\u062E','\u062F','\u0630','\u0631','\u0632','\u0633','\u0634','\u0635','\u0636','\u0637','\u0638','\u0639','\u063A','\u0641','\u0642','\u0643','\u0644','\u0645','\u0646','\u0647','\u0648','\u064A','\u0649','\u0623','\u0625','\u0622','\u0629','\u0640'
    };
    private static final char[] FINAL = {
        '\uFE8E','\uFE90','\uFE92','\uFE96','\uFE9A','\uFE9E','\uFEA2','\uFEA6','\uFEAA','\uFEAC','\uFEAE','\uFEB0','\uFEB2','\uFEB6','\uFEBA','\uFEBE','\uFEC2','\uFEC6','\uFECA','\uFECE','\uFED2','\uFED6','\uFEDA','\uFEDE','\uFEE2','\uFEE6','\uFEEA','\uFEEE','\uFEF2','\uFEF4','\uFE84','\uFE88','\uFE82','\uFE94','\u0640'
    };
    private static final char[] INITIAL = {
        '\uFE8D','\uFE91','\uFE93','\uFE97','\uFE9B','\uFE9F','\uFEA3','\uFEA7','\uFEAB','\uFEAD','\uFEAF','\uFEB1','\uFEB3','\uFEB7','\uFEBB','\uFEBF','\uFEC3','\uFEC7','\uFECB','\uFECF','\uFED3','\uFED7','\uFEDB','\uFEDF','\uFEE3','\uFEE7','\uFEEB','\uFEEF','\uFEF3','\uFEF5','\uFE83','\uFE87','\uFE81','\uFE93','\u0640'
    };
    private static final char[] MEDIAL = {
        '\uFE8D','\uFE92','\uFE94','\uFE98','\uFE9C','\uFEA0','\uFEA4','\uFEA8','\uFEAC','\uFEB0','\uFEB4','\uFEB8','\uFEBC','\uFEC0','\uFEC4','\uFEC8','\uFECC','\uFED0','\uFED4','\uFED8','\uFEDC','\uFEE0','\uFEE4','\uFEE8','\uFEEC','\uFEF0','\uFEF4','\uFEF8','\uFEFC','\uFEF6','\uFE86','\uFE8A','\uFE80','\uFE92','\u0640'
    };

    public static String reshape(String input) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder();
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            char c = chars[i];
            int idx = -1;
            for (int j = 0; j < ISOLATED.length; j++) {
                if (ISOLATED[j] == c) {
                    idx = j;
                    break;
                }
            }
            if (idx != -1) {
                // استخدم الشكل النهائي (final) للحرف
                out.append(FINAL[idx]);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
} 