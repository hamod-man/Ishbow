package com.xowl.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DownloadsActivity extends Activity {
    private static final String ROOT = "Download/Ishbow/";

    private static final class Item {
        Uri uri;
        String name, mime;
        long size, date;
        DownloadCat cat;
    }

    private Prefs prefs;
    private LinearLayout content;
    private TextView titleView;
    private DownloadCat current = null;
    private List<Item> items = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        getWindow().setStatusBarColor(Ui.SURFACE);
        getWindow().setNavigationBarColor(Ui.BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        LinearLayout head = Ui.header(this, "التنزيلات", v -> onBackPressed());
        titleView = (TextView) head.getChildAt(1);
        root.addView(head);

        ScrollView sv = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        sv.addView(content);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
        render();
    }

    @Override
    public void onBackPressed() {
        if (current != null) {
            current = null;
            render();
        } else {
            super.onBackPressed();
        }
    }

    // ------------------------------------------------------------ data

    private void load() {
        items = new ArrayList<>();
        String[] proj = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.RELATIVE_PATH};
        try (Cursor c = getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, proj,
                MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?", new String[]{ROOT + "%"}, null)) {
            if (c == null) return;
            while (c.moveToNext()) {
                Item it = new Item();
                it.uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0));
                it.name = c.getString(1);
                it.size = c.getLong(2);
                it.date = c.getLong(3) * 1000L;
                it.mime = c.getString(4);
                String rel = c.getString(5);
                DownloadCat cat = null;
                if (rel != null && rel.startsWith(ROOT)) {
                    String sub = rel.substring(ROOT.length());
                    int slash = sub.indexOf('/');
                    cat = DownloadCat.fromFolder(slash >= 0 ? sub.substring(0, slash) : sub);
                }
                it.cat = cat != null ? cat : DownloadCat.of(it.name, it.mime);
                if (it.name != null) items.add(it);
            }
        } catch (Exception ignored) {
        }
    }

    private List<Item> itemsOf(DownloadCat cat) {
        List<Item> l = new ArrayList<>();
        for (Item i : items) if (i.cat == cat) l.add(i);
        final boolean byName = prefs.sortBy().equals("name");
        final Collator col = Collator.getInstance();
        Collections.sort(l, new Comparator<Item>() {
            @Override
            public int compare(Item a, Item b) {
                return byName ? col.compare(a.name, b.name) : Long.compare(a.date, b.date);
            }
        });
        if (!prefs.sortAsc()) Collections.reverse(l);
        return l;
    }

    // ------------------------------------------------------------ UI

    private void render() {
        content.removeAllViews();
        if (current == null) renderFolders();
        else renderFiles(current);
    }

    private void renderFolders() {
        titleView.setText("التنزيلات");
        int total = 0;
        for (final DownloadCat cat : DownloadCat.values()) {
            List<Item> l = itemsOf(cat);
            long bytes = 0;
            for (Item i : l) bytes += i.size;
            total += l.size();
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackground(Ui.round(Ui.FIELD, Ui.dp(this, 16)));
            card.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
            TextView ic = new TextView(this);
            ic.setText(cat.icon);
            ic.setTextSize(30);
            ic.setPadding(0, 0, Ui.dp(this, 16), 0);
            card.addView(ic);
            LinearLayout txt = new LinearLayout(this);
            txt.setOrientation(LinearLayout.VERTICAL);
            TextView t = new TextView(this);
            t.setText(cat.label);
            t.setTextColor(Ui.TEXT);
            t.setTextSize(17);
            txt.addView(t);
            TextView s = new TextView(this);
            s.setText(l.size() + " ملف  •  " + Formatter.formatShortFileSize(this, bytes));
            s.setTextColor(Ui.SUB);
            s.setTextSize(13);
            txt.addView(s);
            card.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView arrow = new TextView(this);
            arrow.setText("‹");
            arrow.setTextColor(Ui.SUB);
            arrow.setTextSize(24);
            card.addView(arrow);
            card.setOnClickListener(v -> {
                current = cat;
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), 0);
            content.addView(card, lp);
        }
        if (total == 0) {
            TextView hint = new TextView(this);
            hint.setText("لا توجد تنزيلات بعد.\nتُحفظ الملفات تلقائياً في  Download/Ishbow  داخل مجلد لكل صيغة.");
            hint.setTextColor(Ui.SUB);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(Ui.dp(this, 24), Ui.dp(this, 32), Ui.dp(this, 24), 0);
            content.addView(hint);
        }
    }

    private void renderFiles(DownloadCat cat) {
        titleView.setText(cat.icon + "  " + cat.label);

        // شريط الفرز
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        final boolean byName = prefs.sortBy().equals("name");
        final boolean asc = prefs.sortAsc();
        final TextView by = chip("الفرز: " + (byName ? "الاسم" : "التاريخ") + " ▾");
        final TextView dir = chip("الترتيب: " + (asc ? "تصاعدي ↑" : "تنازلي ↓") + " ▾");
        by.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(this, by);
            pm.getMenu().add(0, 1, 0, (byName ? "" : "✓ ") + "حسب التاريخ");
            pm.getMenu().add(0, 2, 1, (byName ? "✓ " : "") + "حسب الاسم");
            pm.setOnMenuItemClickListener(it -> {
                prefs.setSortBy(it.getItemId() == 2 ? "name" : "date");
                render();
                return true;
            });
            pm.show();
        });
        dir.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(this, dir);
            pm.getMenu().add(0, 1, 0, (asc ? "✓ " : "") + "تصاعدي (الأقدم / أ ← ي)");
            pm.getMenu().add(0, 2, 1, (asc ? "" : "✓ ") + "تنازلي (الأحدث / ي ← أ)");
            pm.setOnMenuItemClickListener(it -> {
                prefs.setSortAsc(it.getItemId() == 1);
                render();
                return true;
            });
            pm.show();
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        bar.addView(by, cp);
        bar.addView(dir, cp);
        content.addView(bar);

        List<Item> l = itemsOf(cat);
        if (l.isEmpty()) {
            TextView e = new TextView(this);
            e.setText("المجلد فارغ");
            e.setTextColor(Ui.SUB);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, Ui.dp(this, 48), 0, 0);
            content.addView(e);
            return;
        }
        for (final Item it : l) content.addView(fileRow(it));
    }

    private TextView chip(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Ui.TEXT);
        t.setTextSize(13);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.round(Ui.FIELD, Ui.dp(this, 18)));
        t.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
        return t;
    }

    private View fileRow(final Item it) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10));
        TextView ic = new TextView(this);
        ic.setText(it.cat.icon);
        ic.setTextSize(24);
        ic.setPadding(0, 0, Ui.dp(this, 14), 0);
        row.addView(ic);
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        TextView n = new TextView(this);
        n.setText(it.name);
        n.setTextColor(Ui.TEXT);
        n.setTextSize(15);
        n.setMaxLines(2);
        n.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        txt.addView(n);
        TextView s = new TextView(this);
        s.setText(Formatter.formatShortFileSize(this, it.size) + "  •  " + fmt.format(new Date(it.date)));
        s.setTextColor(Ui.SUB);
        s.setTextSize(12);
        txt.addView(s);
        row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView more = Ui.btn(this, "⋮", 20, null);
        more.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(this, more);
            pm.getMenu().add(0, 1, 0, "فتح");
            pm.getMenu().add(0, 2, 1, "مشاركة");
            pm.getMenu().add(0, 3, 2, "حذف");
            pm.setOnMenuItemClickListener(m -> {
                if (m.getItemId() == 1) open(it);
                else if (m.getItemId() == 2) share(it);
                else confirmDelete(it);
                return true;
            });
            pm.show();
        });
        row.addView(more);
        row.setOnClickListener(v -> open(it));
        return row;
    }

    private void open(Item it) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(it.uri, it.mime != null ? it.mime : "*/*");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "فتح باستخدام"));
        } catch (Exception e) {
            Toast.makeText(this, "لا يوجد تطبيق مناسب لفتح الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private void share(Item it) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(it.mime != null ? it.mime : "*/*");
        i.putExtra(Intent.EXTRA_STREAM, it.uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "مشاركة"));
    }

    private void confirmDelete(final Item it) {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("حذف الملف؟")
                .setMessage(it.name)
                .setPositiveButton("حذف", (d, w) -> {
                    try {
                        getContentResolver().delete(it.uri, null, null);
                    } catch (Exception e) {
                        Toast.makeText(this, "تعذّر الحذف", Toast.LENGTH_SHORT).show();
                    }
                    load();
                    render();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
}
