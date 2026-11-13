package com.xowl.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class ScriptInstaller {
    private ScriptInstaller() {}

    static void promptInstall(final Activity a, final String url, final Runnable onDone) {
        Toast.makeText(a, "جارِ تحميل السكربت...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                final String code = fetch(url);
                final UserScript s = new UserScript();
                s.code = code;
                s.sourceUrl = url;
                s.parse();
                a.runOnUiThread(() -> confirm(a, s, onDone));
            } catch (final Exception e) {
                a.runOnUiThread(() ->
                        Toast.makeText(a, "فشل التحميل: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private static void confirm(final Activity a, final UserScript s, final Runnable onDone) {
        StringBuilder msg = new StringBuilder();
        if (!s.version.isEmpty()) msg.append("الإصدار: ").append(s.version).append("\n");
        msg.append("يعمل على: ");
        if (s.matchStrings.isEmpty()) msg.append("كل المواقع");
        else msg.append(android.text.TextUtils.join("\n", s.matchStrings));
        new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("تثبيت «" + s.name + "»؟")
                .setMessage(msg.toString())
                .setPositiveButton("تثبيت", (d, w) -> {
                    ScriptStore store = new ScriptStore(a);
                    List<UserScript> all = store.list();
                    for (UserScript o : all) {
                        if (!s.sourceUrl.isEmpty() && s.sourceUrl.equals(o.sourceUrl)) { s.id = o.id; break; }
                    }
                    store.upsert(s);
                    Toast.makeText(a, "تم التثبيت", Toast.LENGTH_SHORT).show();
                    if (onDone != null) onDone.run();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private static String fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(true);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > 2_000_000) throw new Exception("الملف كبير جداً");
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }
}
