package com.xowl.browser;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/** جسر آمن بين السكربتات والتطبيق. كل نداء يتطلب توكن عشوائي لا تعرفه صفحات الويب. */
public final class ScriptBridge {
    private final Activity act;
    private final ScriptStore store;
    private final String token;

    public ScriptBridge(Activity act, ScriptStore store, String token) {
        this.act = act;
        this.store = store;
        this.token = token;
    }

    private boolean ok(String t) { return token.equals(t); }

    @JavascriptInterface
    public String getValue(String t, String id, String k) { return ok(t) ? store.getValue(id, k) : null; }

    @JavascriptInterface
    public void setValue(String t, String id, String k, String v) { if (ok(t)) store.setValue(id, k, v); }

    @JavascriptInterface
    public void deleteValue(String t, String id, String k) { if (ok(t)) store.deleteValue(id, k); }

    @JavascriptInterface
    public String listValues(String t, String id) { return ok(t) ? store.listValues(id) : "[]"; }

    @JavascriptInterface
    public void log(String t, String msg) { if (ok(t)) Log.i("XOwlScript", msg); }

    @JavascriptInterface
    public void copy(String t, final String text) {
        if (!ok(t)) return;
        act.runOnUiThread(() -> {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("XOwl", text));
            Toast.makeText(act, "تم النسخ", Toast.LENGTH_SHORT).show();
        });
    }
}
