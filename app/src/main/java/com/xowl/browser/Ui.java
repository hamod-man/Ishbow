package com.xowl.browser;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

final class Ui {
    static final int BG = 0xFF121212, SURFACE = 0xFF1E1E1E, TEXT = 0xFFEEEEEE, SUB = 0xFF9E9E9E, ACCENT = 0xFFFF9800;

    private Ui() {}

    static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static GradientDrawable round(int color, int radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static TextView btn(Context c, String t, float sp, View.OnClickListener l) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(TEXT);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        v.setOnClickListener(l);
        return v;
    }

    static LinearLayout header(Context c, String title, View.OnClickListener back) {
        LinearLayout h = new LinearLayout(c);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setBackgroundColor(SURFACE);
        h.setPadding(dp(c, 8), dp(c, 8), dp(c, 8), dp(c, 8));
        h.addView(btn(c, "←", 20, back));
        TextView t = new TextView(c);
        t.setText(title);
        t.setTextColor(TEXT);
        t.setTextSize(18);
        h.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return h;
    }

    static LinearLayout row(Context c, String title, String sub, View.OnClickListener l) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        TextView t = new TextView(c);
        t.setText(title);
        t.setTextColor(TEXT);
        t.setTextSize(16);
        r.addView(t);
        if (sub != null) {
            TextView s = new TextView(c);
            s.setText(sub);
            s.setTextColor(SUB);
            s.setTextSize(13);
            r.addView(s);
        }
        if (l != null) r.setOnClickListener(l);
        return r;
    }

    static View toggleRow(Context c, String title, String sub, boolean on, CompoundButton.OnCheckedChangeListener l) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        LinearLayout txt = new LinearLayout(c);
        txt.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(c);
        t.setText(title);
        t.setTextColor(TEXT);
        t.setTextSize(16);
        txt.addView(t);
        if (sub != null) {
            TextView s = new TextView(c);
            s.setText(sub);
            s.setTextColor(SUB);
            s.setTextSize(13);
            txt.addView(s);
        }
        r.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(c);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener(l);
        r.addView(sw);
        return r;
    }
}
