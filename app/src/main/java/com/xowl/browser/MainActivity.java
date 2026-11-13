package com.xowl.browser;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQ_FILE = 1;

    private static final String PRELUDE =
            "var unsafeWindow=window;"
                    + "function GM_getValue(k,d){var v=B.getValue(__t,__id,String(k));if(v===null||v===undefined)return d;try{return JSON.parse(v);}catch(e){return d;}}"
                    + "function GM_setValue(k,v){B.setValue(__t,__id,String(k),JSON.stringify(v));}"
                    + "function GM_deleteValue(k){B.deleteValue(__t,__id,String(k));}"
                    + "function GM_listValues(){try{return JSON.parse(B.listValues(__t,__id));}catch(e){return [];}}"
                    + "function GM_addStyle(css){var s=document.createElement('style');s.textContent=css;(document.head||document.documentElement).appendChild(s);return s;}"
                    + "function GM_log(){B.log(__t,Array.prototype.join.call(arguments,' '));}"
                    + "function GM_openInTab(u){window.open(u,'_blank');}"
                    + "function GM_setClipboard(t){B.copy(__t,String(t));}"
                    + "function GM_registerMenuCommand(){}"
                    + "function GM_xmlhttpRequest(d){var x=new XMLHttpRequest();x.open(d.method||'GET',d.url,true);"
                    + "if(d.headers)for(var h in d.headers)try{x.setRequestHeader(h,d.headers[h]);}catch(e){}"
                    + "x.onload=function(){d.onload&&d.onload({status:x.status,statusText:x.statusText,responseText:x.responseText,response:x.response,responseHeaders:x.getAllResponseHeaders(),finalUrl:d.url});};"
                    + "x.onerror=function(e){d.onerror&&d.onerror(e);};x.send(d.data||null);return {abort:function(){x.abort();}};}"
                    + "var GM={getValue:function(k,d){return Promise.resolve(GM_getValue(k,d));},"
                    + "setValue:function(k,v){return Promise.resolve(GM_setValue(k,v));},"
                    + "deleteValue:function(k){return Promise.resolve(GM_deleteValue(k));},"
                    + "addStyle:GM_addStyle,openInTab:GM_openInTab,setClipboard:GM_setClipboard,xmlHttpRequest:GM_xmlhttpRequest};";


    private static final String MEDIA_JS =
            "(function(){var t=TOKEN;var k='__xom_'+t;if(window[k])return;window[k]=1;"
                    + "function f(x,y,g){var a=document.elementsFromPoint?document.elementsFromPoint(x,y):[g];"
                    + "for(var i=0;i<a.length;i++){var e=a[i],n=e.tagName;"
                    + "if(n==='VIDEO'){var u=e.currentSrc||e.src;if(!u){var s=e.querySelector('source');u=s?s.src:'';}return ['video',u];}"
                    + "if(n==='IMG'){return ['image',e.currentSrc||e.src];}"
                    + "if(e.children.length===0&&e.textContent&&e.textContent.trim().length>0)return null;}return null;}"
                    + "window.addEventListener('contextmenu',function(ev){var r=f(ev.clientX,ev.clientY,ev.target);"
                    + "if(!r||!r[1])return;ev.preventDefault();ev.stopPropagation();"
                    + "window.XOwlMedia.onMedia(t,r[0],r[1],location.href);},true);})();";

    private WebView web;
    private EditText urlBar;
    private ProgressBar progress;
    private TextView reloadBtn;
    private Prefs prefs;
    private ScriptStore store;
    private final String token = UUID.randomUUID().toString();
    private ValueCallback<Uri[]> fileCb;
    private Dialog mediaDialog;
    private boolean desktop = false;
    private String mobileUA;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        store = new ScriptStore(this);
        getWindow().setStatusBarColor(Ui.SURFACE);
        getWindow().setNavigationBarColor(Ui.SURFACE);
        buildUi();
        setupWeb();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handleIntent(i);
    }

    private void handleIntent(Intent i) {
        Uri d = i == null ? null : i.getData();
        if (d != null) web.loadUrl(d.toString());
        else if (web.getUrl() == null) loadHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySettings();
    }

    // ---------------------------------------------------------------- UI

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(Ui.SURFACE);
        top.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));

        urlBar = new EditText(this);
        urlBar.setSingleLine(true);
        urlBar.setTextColor(Ui.TEXT);
        urlBar.setHintTextColor(Ui.SUB);
        urlBar.setHint("ابحث أو اكتب عنواناً");
        urlBar.setTextSize(15);
        urlBar.setBackground(Ui.round(Ui.FIELD, Ui.dp(this, 22)));
        urlBar.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        urlBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlBar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlBar.setSelectAllOnFocus(true);
        urlBar.setOnEditorActionListener((v, a, e) -> {
            go(urlBar.getText().toString());
            return true;
        });
        top.addView(urlBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(top);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(ColorStateList.valueOf(Ui.ACCENT));
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 3)));

        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setBackgroundColor(Ui.SURFACE);
        bottom.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bottom.addView(Ui.btn(this, "◀", 20, v -> { if (web.canGoBack()) web.goBack(); }), lp);
        bottom.addView(Ui.btn(this, "▶", 20, v -> { if (web.canGoForward()) web.goForward(); }), lp);
        reloadBtn = Ui.btn(this, "⟳", 20, v -> {
            if (progress.getVisibility() == View.VISIBLE) web.stopLoading();
            else web.reload();
        });
        bottom.addView(reloadBtn, lp);
        bottom.addView(Ui.btn(this, "⌂", 22, v -> loadHome()), lp);
        final TextView menu = Ui.btn(this, "⋮", 22, null);
        menu.setOnClickListener(v -> showMenu(menu));
        bottom.addView(menu, lp);
        root.addView(bottom);

        setContentView(root);
    }

    private void showMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, "سكربتات المستخدم");
        pm.getMenu().add(0, 2, 1, "الإعدادات");
        pm.getMenu().add(0, 3, 2, desktop ? "✓ وضع سطح المكتب" : "وضع سطح المكتب");
        pm.getMenu().add(0, 4, 3, "مشاركة الرابط");
        pm.getMenu().add(0, 5, 4, "نسخ الرابط");
        pm.setOnMenuItemClickListener(it -> {
            switch (it.getItemId()) {
                case 1:
                    startActivity(new Intent(this, ScriptsActivity.class));
                    break;
                case 2:
                    startActivity(new Intent(this, SettingsActivity.class));
                    break;
                case 3:
                    desktop = !desktop;
                    applySettings();
                    web.reload();
                    break;
                case 4: {
                    String u = web.getUrl();
                    if (u != null && u.startsWith("http")) {
                        Intent s = new Intent(Intent.ACTION_SEND);
                        s.setType("text/plain");
                        s.putExtra(Intent.EXTRA_TEXT, u);
                        startActivity(Intent.createChooser(s, "مشاركة"));
                    }
                    break;
                }
                case 5: {
                    String u = web.getUrl();
                    if (u != null && u.startsWith("http")) {
                        ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
                                .setPrimaryClip(ClipData.newPlainText("url", u));
                        Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                default:
                    break;
            }
            return true;
        });
        pm.show();
    }

    // ---------------------------------------------------------------- WebView

    private void setupWeb() {
        WebSettings s = web.getSettings();
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setGeolocationEnabled(false);
        s.setSafeBrowsingEnabled(false);
        s.setSupportMultipleWindows(false);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        mobileUA = s.getUserAgentString().replace("; wv", "");

        web.addJavascriptInterface(new ScriptBridge(this, store, token), "XOwlBridge");
        CookieManager.getInstance().setAcceptCookie(true);
        applySettings();

        web.setDownloadListener((url, ua, cd, mime, len) -> {
            if (url.matches("(?i).*\\.user\\.js([?#].*)?$")) {
                ScriptInstaller.promptInstall(this, url, null);
                return;
            }
            enqueueDownload(url, ua, cd, mime, web.getUrl(), null);
        });

        web.addJavascriptInterface(new MediaBridge(), "XOwlMedia");
        web.setOnLongClickListener(v -> {
            if (prefs.js()) return false; // عند تفعيل JS تتولى السكربت الكشف عن الوسائط
            WebView.HitTestResult h = web.getHitTestResult();
            int t = h.getType();
            if ((t == WebView.HitTestResult.IMAGE_TYPE || t == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)
                    && h.getExtra() != null) {
                showMediaMenu("image", h.getExtra(), web.getUrl());
                return true;
            }
            return false;
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                progress.setProgress(p);
                progress.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
                reloadBtn.setText(p < 100 ? "✕" : "⟳");
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try {
                    startActivityForResult(p.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String url = u.toString();
                if (url.matches("(?i).*\\.user\\.js([?#].*)?$")) {
                    ScriptInstaller.promptInstall(MainActivity.this, url, null);
                    return true;
                }
                String sc = u.getScheme();
                if (sc == null) return false;
                if (sc.equals("http") || sc.equals("https") || sc.equals("about")
                        || sc.equals("data") || sc.equals("blob") || sc.equals("javascript")) return false;
                if (sc.equals("intent")) return true;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                if (!prefs.blockGoogle()) return null;
                String host = r.getUrl().getHost();
                if (host != null && GoogleBlocker.isGoogle(host)) {
                    if (r.isForMainFrame()) {
                        String html = "<html dir='rtl'><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                                + "<style>body{background:#14213D;color:#eee;font-family:sans-serif;text-align:center;padding:15vh 24px}"
                                + "h1{font-size:64px;margin:0}small{color:#999}</style></head><body><h1>🦉</h1>"
                                + "<h2>تم الحظر</h2><p>" + host.replaceAll("[<>&\"']", "") + "</p>"
                                + "<small>Ishbow: لا لجوجل. يمكنك السماح من الإعدادات.</small></body></html>";
                        return new WebResourceResponse("text/html", "utf-8",
                                new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
                    }
                    return new WebResourceResponse("text/plain", "utf-8", 403, "Blocked",
                            new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
                }
                return null;
            }

            @Override
            public void onPageStarted(WebView v, String url, Bitmap icon) {
                updateUrl(url);
                injectMedia(url);
                inject(url, true);
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                updateUrl(url);
                injectMedia(url);
                inject(url, false);
            }
        });
    }

    private void applySettings() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(prefs.js());
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, prefs.thirdPartyCookies());
        if (desktop) {
            s.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Safari/537.36");
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
        } else {
            s.setUserAgentString(mobileUA);
            s.setUseWideViewPort(false);
            s.setLoadWithOverviewMode(false);
        }
    }

    // ---------------------------------------------------------------- Scripts

    /**
     * start=true  : onPageStarted — يشغّل سكربتات document-start (بأفضل جهد).
     * start=false : onPageFinished — يشغّل البقية، وأي document-start لم يعمل بعد.
     * الحارس داخل الغلاف يمنع التشغيل المزدوج في نفس الصفحة.
     */
    private void inject(String url, boolean start) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) return;
        if (!prefs.js()) return;
        for (UserScript s : store.list()) {
            if (!s.enabled || !s.appliesTo(url)) continue;
            boolean isStart = s.runAt.equals("document-start");
            if (start && !isStart) continue;
            web.evaluateJavascript(wrap(s), null);
        }
    }

    private String wrap(UserScript s) {
        StringBuilder sb = new StringBuilder();
        sb.append("(function(){var __id=").append(JSONObject.quote(s.id))
                .append(",__t=").append(JSONObject.quote(token)).append(";")
                .append("var __f='__xo_'+__t+__id;if(window[__f])return;window[__f]=1;")
                .append("var B=window.XOwlBridge;if(!B)return;")
                .append(PRELUDE)
                .append("var GM_info={script:{name:").append(JSONObject.quote(s.name))
                .append(",version:").append(JSONObject.quote(s.version)).append("}};")
                .append("try{(function(){\n").append(s.code).append("\n}).call(window);}")
                .append("catch(e){B.log(__t,").append(JSONObject.quote("[" + s.name + "] "))
                .append("+e);}})();");
        return sb.toString();
    }


    // ---------------------------------------------------------------- Media (long press)

    private void injectMedia(String url) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) return;
        if (!prefs.js()) return;
        web.evaluateJavascript(MEDIA_JS.replace("TOKEN", JSONObject.quote(token)), null);
    }

    public final class MediaBridge {
        @JavascriptInterface
        public void onMedia(String t, final String type, final String url, final String page) {
            if (!token.equals(t)) return;
            runOnUiThread(() -> showMediaMenu(type, url, page));
        }
    }

    private TextView menuItem(String text, final Runnable action) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(16);
        t.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 14));
        t.setOnClickListener(v -> {
            if (mediaDialog != null) mediaDialog.dismiss();
            action.run();
        });
        return t;
    }

    private void showMediaMenu(final String type, final String url, final String page) {
        if (url == null || url.isEmpty()) return;
        if (mediaDialog != null && mediaDialog.isShowing()) return;
        final boolean video = type.equals("video");
        final boolean downloadable = url.startsWith("http://") || url.startsWith("https://");

        final Dialog d = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        mediaDialog = d;

        LinearLayout outer = new LinearLayout(this);
        outer.setGravity(Gravity.BOTTOM);
        outer.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 16));
        outer.setOnClickListener(v -> d.dismiss());

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setClickable(true);
        android.graphics.drawable.GradientDrawable bg = Ui.round(0x7314213D, Ui.dp(this, 20)); // شفافية 45%
        bg.setStroke(Ui.dp(this, 1), 0x33FFFFFF);
        panel.setBackground(bg);

        TextView title = new TextView(this);
        title.setText(video ? "فيديو" : "صورة");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(13);
        title.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), 0);
        panel.addView(title);
        TextView sub = new TextView(this);
        sub.setText(url.startsWith("data:") ? "data:..." : url);
        sub.setTextColor(0xCCFFFFFF);
        sub.setTextSize(12);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        sub.setPadding(Ui.dp(this, 20), Ui.dp(this, 2), Ui.dp(this, 20), Ui.dp(this, 6));
        panel.addView(sub);

        panel.addView(menuItem("⬇  تحميل", () -> {
            if (!downloadable) {
                Toast.makeText(this, "لا يمكن تنزيل هذا الرابط (بث أو blob)", Toast.LENGTH_LONG).show();
                return;
            }
            enqueueDownload(url, web.getSettings().getUserAgentString(), null, null, page, type);
        }));
        panel.addView(menuItem("⧉  نسخ الرابط", () -> {
            ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("url", url));
            Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show();
        }));
        panel.addView(menuItem("➦  مشاركة", () -> {
            Intent sh = new Intent(Intent.ACTION_SEND);
            sh.setType("text/plain");
            sh.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(sh, "مشاركة"));
        }));
        panel.addView(menuItem("▣  فتح في صفحة خاصة", () -> {
            Intent i = new Intent(this, MediaViewerActivity.class);
            i.putExtra(MediaViewerActivity.EXTRA_URL, url);
            i.putExtra(MediaViewerActivity.EXTRA_TYPE, type);
            i.putExtra(MediaViewerActivity.EXTRA_PAGE, page);
            startActivity(i);
        }));
        panel.addView(menuItem("↗  فتح في تطبيق آخر", () -> {
            if (!downloadable) {
                Toast.makeText(this, "لا يمكن فتح هذا الرابط خارج المتصفح", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.parse(url), video ? "video/*" : "image/*");
                startActivity(Intent.createChooser(i, "فتح باستخدام"));
            } catch (Exception e) {
                Toast.makeText(this, "لا يوجد تطبيق مناسب", Toast.LENGTH_SHORT).show();
            }
        }));

        outer.addView(panel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        d.setContentView(outer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        d.getWindow().setDimAmount(0.2f);
        d.show();
    }

    private void enqueueDownload(String url, String ua, String cd, String mime, String referer, String typeHint) {
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            if (mime != null) r.setMimeType(mime);
            if (ua != null) r.addRequestHeader("User-Agent", ua);
            if (referer != null && referer.startsWith("http")) r.addRequestHeader("Referer", referer);
            String ck = CookieManager.getInstance().getCookie(url);
            if (ck != null) r.addRequestHeader("Cookie", ck);
            String fn = URLUtil.guessFileName(url, cd, mime);
            if (typeHint != null && (fn.endsWith(".bin") || !fn.contains("."))) {
                fn = (fn.endsWith(".bin") ? fn.substring(0, fn.length() - 4) : fn)
                        + (typeHint.equals("video") ? ".mp4" : ".jpg");
            }
            r.setTitle(fn);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fn);
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);
            Toast.makeText(this, "بدأ التنزيل: " + fn, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذّر التنزيل", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------- Navigation

    private void updateUrl(String url) {
        if (urlBar.hasFocus()) return;
        if (url == null || url.startsWith("about:") || url.startsWith("data:")) urlBar.setText("");
        else urlBar.setText(url);
    }

    private void go(String in) {
        in = in.trim();
        if (in.isEmpty()) return;
        String url;
        if (in.equals("about:home")) {
            loadHome();
            hideKeyboard();
            return;
        } else if (in.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            url = in;
        } else if (!in.contains(" ") && (in.contains(".") || in.startsWith("localhost"))) {
            url = "https://" + in;
        } else {
            try {
                url = prefs.engine()[1].replace("%s", URLEncoder.encode(in, "UTF-8"));
            } catch (Exception e) {
                return;
            }
        }
        web.loadUrl(url);
        hideKeyboard();
        web.requestFocus();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        urlBar.clearFocus();
    }

    private String logoUri() {
        try (java.io.InputStream in = getAssets().open("logo.png")) {
            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
            return "data:image/png;base64," + android.util.Base64.encodeToString(o.toByteArray(), android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private void loadHome() {
        String t = prefs.engine()[1];
        int q = t.indexOf('?');
        String action = q > 0 ? t.substring(0, q) : t;
        String html = "<html dir='rtl'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{background:#14213D;color:#eee;font-family:sans-serif;text-align:center;padding-top:24vh}"
                + ""
                + "input{width:80%;max-width:420px;margin-top:36px;padding:16px 22px;border-radius:26px;border:0;"
                + "background:#313a4e;color:#eee;font-size:16px;outline:none}"
                + "p{color:#fff;font:italic bold 22px Georgia,serif;margin-top:32px}</style></head><body>"
                + "<img src='" + logoUri() + "' style='width:270px;max-width:80%'>"
                + "<form action='" + action + "' method='get'><input name='q' placeholder='ابحث في "
                + prefs.engine()[0].replaceAll("[<>&\"']", "") + "' autocomplete='off'></form>"
                + "<p>#nogoogle</p></body></html>";
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        urlBar.setText("");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCb = null;
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
