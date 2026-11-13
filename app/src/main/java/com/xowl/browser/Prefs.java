package com.xowl.browser;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    /** {الاسم، قالب البحث، } — آخر عنصر هو Google وهو اختياري ولا يُفعَّل إلا بإرادة المستخدم */
    public static final String[][] ENGINES = {
            {"DuckDuckGo", "https://duckduckgo.com/?q=%s"},
            {"Brave Search", "https://search.brave.com/search?q=%s"},
            {"Startpage", "https://www.startpage.com/do/search?q=%s"},
            {"Qwant", "https://www.qwant.com/?q=%s"},
            {"Ecosia", "https://www.ecosia.org/search?q=%s"},
            {"Mojeek", "https://www.mojeek.com/search?q=%s"},
            {"Google (اختياري)", "https://www.google.com/search?q=%s"},
    };

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("xowl", Context.MODE_PRIVATE);
    }

    public int engineIndex() {
        int i = sp.getInt("engine", 0);
        return (i >= 0 && i < ENGINES.length) ? i : 0;
    }

    public void setEngineIndex(int i) { sp.edit().putInt("engine", i).apply(); }

    public String[] engine() { return ENGINES[engineIndex()]; }

    public static boolean isGoogleEngine(int i) { return i == ENGINES.length - 1; }

    public boolean blockGoogle() { return sp.getBoolean("blockGoogle", true); }
    public void setBlockGoogle(boolean v) { sp.edit().putBoolean("blockGoogle", v).apply(); }

    public boolean js() { return sp.getBoolean("js", true); }
    public void setJs(boolean v) { sp.edit().putBoolean("js", v).apply(); }

    public boolean thirdPartyCookies() { return sp.getBoolean("tpc", false); }
    public void setThirdPartyCookies(boolean v) { sp.edit().putBoolean("tpc", v).apply(); }

    /** "date" أو "name" */
    public String sortBy() { return sp.getString("sortBy", "date"); }
    public void setSortBy(String v) { sp.edit().putString("sortBy", v).apply(); }

    public boolean sortAsc() { return sp.getBoolean("sortAsc", false); }
    public void setSortAsc(boolean v) { sp.edit().putBoolean("sortAsc", v).apply(); }
}
