package com.xowl.browser;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ScriptStore {
    private final File file;
    private final SharedPreferences vals;

    public ScriptStore(Context c) {
        c = c.getApplicationContext();
        file = new File(c.getFilesDir(), "scripts.json");
        vals = c.getSharedPreferences("xowl_values", Context.MODE_PRIVATE);
    }

    public synchronized List<UserScript> list() {
        List<UserScript> out = new ArrayList<>();
        try {
            if (!file.exists()) return out;
            String s = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) out.add(UserScript.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {
        }
        return out;
    }

    public synchronized void saveAll(List<UserScript> l) {
        try {
            JSONArray a = new JSONArray();
            for (UserScript u : l) a.put(u.toJson());
            Files.write(file.toPath(), a.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    public synchronized void upsert(UserScript s) {
        List<UserScript> l = list();
        boolean found = false;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id.equals(s.id)) { l.set(i, s); found = true; break; }
        }
        if (!found) l.add(s);
        saveAll(l);
    }

    public synchronized void delete(String id) {
        List<UserScript> l = list();
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id.equals(id)) { l.remove(i); break; }
        }
        saveAll(l);
        SharedPreferences.Editor e = vals.edit();
        for (String k : vals.getAll().keySet()) if (k.startsWith(id + "\u0000")) e.remove(k);
        e.apply();
    }

    public String getValue(String id, String key) { return vals.getString(id + "\u0000" + key, null); }
    public void setValue(String id, String key, String v) { vals.edit().putString(id + "\u0000" + key, v).apply(); }
    public void deleteValue(String id, String key) { vals.edit().remove(id + "\u0000" + key).apply(); }

    public String listValues(String id) {
        JSONArray a = new JSONArray();
        for (Map.Entry<String, ?> e : vals.getAll().entrySet()) {
            if (e.getKey().startsWith(id + "\u0000")) a.put(e.getKey().substring(id.length() + 1));
        }
        return a.toString();
    }
}
