package com.xowl.browser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UserScript {
    public String id = UUID.randomUUID().toString();
    public String code = "";
    public String sourceUrl = "";
    public boolean enabled = true;

    public String name = "Untitled";
    public String version = "";
    public String runAt = "document-end";
    public final List<String> matchStrings = new ArrayList<>();

    private final List<Pattern> includes = new ArrayList<>();
    private final List<Pattern> excludes = new ArrayList<>();

    /** يقرأ ترويسة ==UserScript== من الكود. */
    public void parse() {
        includes.clear();
        excludes.clear();
        matchStrings.clear();
        name = "Untitled";
        version = "";
        runAt = "document-end";

        int a = code.indexOf("==UserScript==");
        int b = code.indexOf("==/UserScript==");
        if (a < 0 || b <= a) return;
        String block = code.substring(a + 14, b);
        Pattern line = Pattern.compile("^\\s*//\\s*@([\\w:-]+)\\s*(.*?)\\s*$");
        for (String l : block.split("\\r?\\n")) {
            Matcher m = line.matcher(l);
            if (!m.matches()) continue;
            String k = m.group(1).toLowerCase(Locale.ROOT);
            String v = m.group(2);
            switch (k) {
                case "name":
                    if (!v.isEmpty()) name = v;
                    break;
                case "version":
                    version = v;
                    break;
                case "run-at":
                    runAt = v;
                    break;
                case "match":
                case "include": {
                    matchStrings.add(v);
                    Pattern p = glob(v);
                    if (p != null) includes.add(p);
                    break;
                }
                case "exclude":
                case "exclude-match": {
                    Pattern p = glob(v);
                    if (p != null) excludes.add(p);
                    break;
                }
                default:
                    break;
            }
        }
    }

    public boolean appliesTo(String url) {
        if (url == null) return false;
        boolean ok = matchStrings.isEmpty(); // بدون @match => كل المواقع
        for (Pattern p : includes) {
            if (p.matcher(url).matches()) { ok = true; break; }
        }
        if (!ok) return false;
        for (Pattern p : excludes) {
            if (p.matcher(url).matches()) return false;
        }
        return true;
    }

    static Pattern glob(String g) {
        try {
            if (g.equals("<all_urls>") || g.equals("*")) return Pattern.compile(".*");
            if (g.length() > 2 && g.startsWith("/") && g.endsWith("/")) {
                return Pattern.compile(g.substring(1, g.length() - 1));
            }
            String s = g.replace("*://*.", "\u0003").replace("*://", "\u0001").replace("://*.", "\u0002");
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (c == '\u0003') sb.append("https?://(?:[^/]*\\.)?");
                else if (c == '\u0001') sb.append("https?://");
                else if (c == '\u0002') sb.append("://(?:[^/]*\\.)?");
                else if (c == '*') sb.append(".*");
                else {
                    if ("\\.[]{}()+-^$|?".indexOf(c) >= 0) sb.append('\\');
                    sb.append(c);
                }
            }
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            return null;
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("code", code);
        o.put("src", sourceUrl);
        o.put("enabled", enabled);
        return o;
    }

    public static UserScript fromJson(JSONObject o) {
        UserScript s = new UserScript();
        s.id = o.optString("id", s.id);
        s.code = o.optString("code", "");
        s.sourceUrl = o.optString("src", "");
        s.enabled = o.optBoolean("enabled", true);
        s.parse();
        return s;
    }
}
