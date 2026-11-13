package com.xowl.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private Prefs p;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);
        getWindow().setStatusBarColor(Ui.SURFACE);
        getWindow().setNavigationBarColor(Ui.BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, "الإعدادات", v -> finish()));
        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        render();
    }

    private void render() {
        list.removeAllViews();
        list.addView(Ui.row(this, "محرك البحث", p.engine()[0], v -> chooseEngine()));
        list.addView(Ui.toggleRow(this, "حظر خدمات Google",
                "يمنع google وgstatic وإعلاناتها وتحليلاتها ويوتيوب وSafe Browsing",
                p.blockGoogle(), (btn, on) -> {
                    p.setBlockGoogle(on);
                    if (on && Prefs.isGoogleEngine(p.engineIndex())) {
                        p.setEngineIndex(0);
                        render();
                    }
                }));
        list.addView(Ui.toggleRow(this, "JavaScript", "تعطيله يوقف السكربتات أيضاً", p.js(),
                (btn, on) -> p.setJs(on)));
        list.addView(Ui.toggleRow(this, "كوكيز الأطراف الثالثة", "معطّلة افتراضياً لحمايتك من التتبع",
                p.thirdPartyCookies(), (btn, on) -> p.setThirdPartyCookies(on)));
        list.addView(Ui.row(this, "التنزيلات", "الملفات مرتبة حسب الصيغة",
                v -> startActivity(new Intent(this, DownloadsActivity.class))));
        list.addView(Ui.row(this, "سكربتات المستخدم", "إدارة وتثبيت السكربتات",
                v -> startActivity(new Intent(this, ScriptsActivity.class))));
        list.addView(Ui.row(this, "مسح بيانات التصفح", "الكوكيز والتخزين والكاش", v -> clearData()));

        TextView about = new TextView(this);
        String ver = "";
        try { ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) { }
        about.setText("Ishbow v" + ver + "\nمتصفح بمبدأ: لا لجوجل");
        about.setTextColor(Ui.SUB);
        about.setPadding(Ui.dp(this, 16), Ui.dp(this, 24), Ui.dp(this, 16), Ui.dp(this, 24));
        list.addView(about);
    }

    private void chooseEngine() {
        String[] names = new String[Prefs.ENGINES.length];
        for (int i = 0; i < names.length; i++) names[i] = Prefs.ENGINES[i][0];
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("محرك البحث")
                .setSingleChoiceItems(names, p.engineIndex(), (d, which) -> {
                    p.setEngineIndex(which);
                    if (Prefs.isGoogleEngine(which) && p.blockGoogle()) {
                        p.setBlockGoogle(false);
                        Toast.makeText(this, "تم إيقاف حظر Google ليعمل المحرك", Toast.LENGTH_LONG).show();
                    }
                    d.dismiss();
                    render();
                })
                .show();
    }

    private void clearData() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        WebView tmp = new WebView(this);
        tmp.clearCache(true);
        tmp.destroy();
        Toast.makeText(this, "تم المسح", Toast.LENGTH_SHORT).show();
    }
}
