package com.xowl.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class ScriptsActivity extends Activity {
    private static final String TEMPLATE =
            "// ==UserScript==\n"
                    + "// @name         سكربت جديد\n"
                    + "// @version      1.0\n"
                    + "// @match        *://*/*\n"
                    + "// @run-at       document-end\n"
                    + "// ==/UserScript==\n\n"
                    + "(function () {\n  'use strict';\n\n})();\n";

    private ScriptStore store;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new ScriptStore(this);
        getWindow().setStatusBarColor(Ui.SURFACE);
        getWindow().setNavigationBarColor(Ui.BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout head = Ui.header(this, "سكربتات المستخدم", v -> finish());
        head.addView(Ui.btn(this, "＋", 20, v -> edit(null)));
        head.addView(Ui.btn(this, "⬇", 20, v -> installFromUrl()));
        root.addView(head);

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        list.removeAllViews();
        List<UserScript> all = store.list();
        if (all.isEmpty()) {
            TextView t = new TextView(this);
            t.setText("لا توجد سكربتات بعد.\n＋ لكتابة سكربت جديد\n⬇ لتثبيت سكربت من رابط .user.js\n"
                    + "ويمكنك أيضاً فتح أي رابط ينتهي بـ .user.js في المتصفح وسيُعرض عليك تثبيته.");
            t.setTextColor(Ui.SUB);
            t.setGravity(Gravity.CENTER);
            t.setPadding(Ui.dp(this, 24), Ui.dp(this, 48), Ui.dp(this, 24), 0);
            list.addView(t);
            return;
        }
        for (final UserScript s : all) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 10));

            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            Switch sw = new Switch(this);
            sw.setText(s.name);
            sw.setTextColor(Ui.TEXT);
            sw.setTextSize(16);
            sw.setChecked(s.enabled);
            sw.setOnCheckedChangeListener((btn, on) -> {
                s.enabled = on;
                store.upsert(s);
            });
            top.addView(sw, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            top.addView(Ui.btn(this, "✎", 18, v -> edit(s)));
            top.addView(Ui.btn(this, "🗑", 18, v -> confirmDelete(s)));
            card.addView(top);

            TextView sub = new TextView(this);
            String m = s.matchStrings.isEmpty() ? "كل المواقع" : android.text.TextUtils.join(" ، ", s.matchStrings);
            sub.setText((s.version.isEmpty() ? "" : "v" + s.version + "  •  ") + m);
            sub.setTextColor(Ui.SUB);
            sub.setTextSize(12);
            sub.setMaxLines(2);
            card.addView(sub);
            list.addView(card);
        }
    }

    private void confirmDelete(final UserScript s) {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("حذف «" + s.name + "»؟")
                .setPositiveButton("حذف", (d, w) -> {
                    store.delete(s.id);
                    render();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void edit(final UserScript existing) {
        final EditText et = new EditText(this);
        et.setText(existing != null ? existing.code : TEMPLATE);
        et.setTypeface(Typeface.MONOSPACE);
        et.setTextSize(12);
        et.setGravity(Gravity.TOP | Gravity.START);
        et.setMinLines(12);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(existing == null ? "سكربت جديد" : "تعديل السكربت")
                .setView(et)
                .setPositiveButton("حفظ", (d, w) -> {
                    UserScript s = existing != null ? existing : new UserScript();
                    s.code = et.getText().toString();
                    s.parse();
                    store.upsert(s);
                    render();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void installFromUrl() {
        final EditText et = new EditText(this);
        et.setHint("https://.../script.user.js");
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("تثبيت من رابط")
                .setView(et)
                .setPositiveButton("تحميل", (d, w) -> {
                    String u = et.getText().toString().trim();
                    if (!u.isEmpty()) ScriptInstaller.promptInstall(this, u, this::render);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
}
