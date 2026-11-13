package com.xowl.browser;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

/** صفحة خاصة لعرض صورة أو فيديو بملء الشاشة مع تكبير بالقرصة. */
public class MediaViewerActivity extends Activity {
    static final String EXTRA_URL = "url", EXTRA_TYPE = "type", EXTRA_PAGE = "page";
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.SURFACE);
        getWindow().setNavigationBarColor(Ui.BG);
        String url = getIntent().getStringExtra(EXTRA_URL);
        String type = getIntent().getStringExtra(EXTRA_TYPE);
        String page = getIntent().getStringExtra(EXTRA_PAGE);
        if (url == null) { finish(); return; }
        boolean video = "video".equals(type);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, video ? "عرض الفيديو" : "عرض الصورة", v -> finish()));

        web = new WebView(this);
        web.setBackgroundColor(Ui.BG);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(false);
        s.setAllowFileAccess(false);
        s.setSafeBrowsingEnabled(false);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        String safe = url.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
        String tag = video ? "<video src=\"" + safe + "\" controls autoplay playsinline></video>"
                : "<img src=\"" + safe + "\">";
        String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>html,body{margin:0;background:#14213D}"
                + "body{display:flex;align-items:center;justify-content:center;min-height:100vh}"
                + "img,video{max-width:100%;max-height:100vh}</style></head><body>" + tag + "</body></html>";
        String base = (page != null && page.startsWith("http")) ? page : url;
        web.loadDataWithBaseURL(base, html, "text/html", "utf-8", null);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
