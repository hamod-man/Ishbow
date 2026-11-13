package com.xowl.browser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** يحدد إن كان المضيف يتبع لخدمات جوجل (بحث، تحليلات، إعلانات، خطوط، يوتيوب...). */
public final class GoogleBlocker {
    private static final Set<String> LABELS = new HashSet<>(Arrays.asList(
            "google", "googleapis", "googleusercontent", "googlevideo", "googlesyndication",
            "googleadservices", "googletagmanager", "googletagservices", "google-analytics",
            "googleanalytics", "gstatic", "ggpht", "doubleclick", "youtube", "youtu", "ytimg",
            "withgoogle", "gvt1", "gvt2", "googlezip", "googleadapis", "admob", "goo"));
    private static final Set<String> SLD = new HashSet<>(Arrays.asList(
            "co", "com", "org", "net", "gov", "edu", "ac"));

    private GoogleBlocker() {}

    public static boolean isGoogle(String host) {
        if (host == null) return false;
        String[] p = host.toLowerCase(Locale.ROOT).split("\\.");
        int n = p.length;
        if (n >= 2 && LABELS.contains(p[n - 2])) return true;          // www.google.com
        if (n >= 3 && SLD.contains(p[n - 2]) && LABELS.contains(p[n - 3])) return true; // google.co.uk
        return false;
    }
}
