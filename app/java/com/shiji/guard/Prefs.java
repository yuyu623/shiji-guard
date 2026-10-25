package com.shiji.guard;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public final class Prefs {

    private static final String NAME = "shiji";
    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public boolean trackerEnabled() {
        return sp.getBoolean("tracker", true);
    }

    public void setTrackerEnabled(boolean v) {
        sp.edit().putBoolean("tracker", v).apply();
    }

    public long lastEventTs() {
        return sp.getLong("last_event", 0L);
    }

    public void setLastEventTs(long v) {
        sp.edit().putLong("last_event", v).apply();
    }

    public String openPkg() {
        return sp.getString("open_pkg", null);
    }

    public long openStart() {
        return sp.getLong("open_start", 0L);
    }

    public void setOpen(String pkg, long start) {
        sp.edit().putString("open_pkg", pkg).putLong("open_start", start).apply();
    }

    public void clearOpen() {
        sp.edit().remove("open_pkg").remove("open_start").apply();
    }

    private static final String MEDIA_OPEN_PREFIX = "media_open_";

    public Map<String, Long> openMedia() {
        Map<String, Long> out = new HashMap<>();
        for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            String key = e.getKey();
            if (key.startsWith(MEDIA_OPEN_PREFIX) && e.getValue() instanceof Long) {
                out.put(key.substring(MEDIA_OPEN_PREFIX.length()), (Long) e.getValue());
            }
        }
        return out;
    }

    public void setOpenMedia(String pkg, long start) {
        sp.edit().putLong(MEDIA_OPEN_PREFIX + pkg, start).apply();
    }

    public void clearOpenMedia(String pkg) {
        sp.edit().remove(MEDIA_OPEN_PREFIX + pkg).apply();
    }

    public String apiKey() {
        return sp.getString("api_key", "");
    }

    public void setApiKey(String v) {
        sp.edit().putString("api_key", v).apply();
    }

    public String baseUrl() {
        String v = sp.getString("base_url", "https://api.deepseek.com");
        return v == null || v.trim().isEmpty() ? "https://api.deepseek.com" : v.trim();
    }

    public void setBaseUrl(String v) {
        sp.edit().putString("base_url", v == null ? "" : v.trim()).apply();
    }

    public String aiStyle() {
        return sp.getString("ai_style", "direct");
    }

    public void setAiStyle(String v) {
        sp.edit().putString("ai_style", v).apply();
    }

    public int aiIntensity() {
        int v = sp.getInt("ai_intensity", 4);
        return Math.max(1, Math.min(7, v));
    }

    public void setAiIntensity(int v) {
        sp.edit().putInt("ai_intensity", Math.max(1, Math.min(7, v))).apply();
    }

    public String aiFocus() {
        return sp.getString("ai_focus", "structure,action");
    }

    public void setAiFocus(String v) {
        sp.edit().putString("ai_focus", v == null ? "" : v).apply();
    }

    public int aiLength() {
        int v = sp.getInt("ai_length", 1);
        return Math.max(0, Math.min(2, v));
    }

    public void setAiLength(int v) {
        sp.edit().putInt("ai_length", Math.max(0, Math.min(2, v))).apply();
    }

    public String aiCustom() {
        String v = sp.getString("ai_custom", "");
        return v == null ? "" : v;
    }

    public void setAiCustom(String v) {
        sp.edit().putString("ai_custom", v == null ? "" : v.trim()).apply();
    }

    public long aiStyleChangedAt() {
        return sp.getLong("ai_style_at", 0L);
    }

    public void setAiStyleChangedAt(long v) {
        sp.edit().putLong("ai_style_at", v).apply();
    }

    public String model() {
        String v = sp.getString("model", "deepseek-chat");
        return v == null || v.trim().isEmpty() ? "deepseek-chat" : v.trim();
    }

    public void setModel(String v) {
        sp.edit().putString("model", v == null ? "" : v.trim()).apply();
    }

    /** 0 = 跟随系统，1 = 浅色，2 = 深色。 */
    public int themeMode() {
        return sp.getInt("theme_mode", 0);
    }

    public void setThemeMode(int v) {
        sp.edit().putInt("theme_mode", v).apply();
    }

    public boolean introSeen() {
        return sp.getBoolean("intro_seen", false);
    }

    public void setIntroSeen() {
        sp.edit().putBoolean("intro_seen", true).apply();
    }
}
