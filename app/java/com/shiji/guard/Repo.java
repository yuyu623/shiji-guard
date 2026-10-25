package com.shiji.guard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Repo extends SQLiteOpenHelper {

    public static class Sess {
        public final String pkg;
        public final long start;
        public final long end;

        public Sess(String pkg, long start, long end) {
            this.pkg = pkg;
            this.start = start;
            this.end = end;
        }
    }

    public static class Rule {
        public String pkg;
        public int limitMin;
        public boolean enabled;
    }

    public Repo(Context c) {
        super(c, "shiji.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sessions(_id INTEGER PRIMARY KEY AUTOINCREMENT, pkg TEXT NOT NULL, start_ts INTEGER NOT NULL, end_ts INTEGER NOT NULL, day TEXT NOT NULL, UNIQUE(pkg,start_ts))");
        db.execSQL("CREATE INDEX idx_sessions_day ON sessions(day)");
        db.execSQL("CREATE INDEX idx_sessions_start ON sessions(start_ts)");
        db.execSQL("CREATE TABLE rules(pkg TEXT PRIMARY KEY, limit_min INTEGER NOT NULL, enabled INTEGER NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE ai(period_key TEXT PRIMARY KEY, text TEXT NOT NULL, ts INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE blocks(_id INTEGER PRIMARY KEY AUTOINCREMENT, pkg TEXT NOT NULL, ts INTEGER NOT NULL, day TEXT NOT NULL)");
        db.execSQL("CREATE INDEX idx_blocks_day ON blocks(day)");
        createMediaTable(db);
    }

    private static void createMediaTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE media_sessions(_id INTEGER PRIMARY KEY AUTOINCREMENT, pkg TEXT NOT NULL, start_ts INTEGER NOT NULL, end_ts INTEGER NOT NULL, day TEXT NOT NULL, UNIQUE(pkg,start_ts))");
        db.execSQL("CREATE INDEX idx_media_day ON media_sessions(day)");
        db.execSQL("CREATE INDEX idx_media_start ON media_sessions(start_ts)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createMediaTable(db);
        }
    }

    public synchronized void addSessions(List<Sess> list) {
        if (list == null || list.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Sess s : list) {
                if (s == null || s.pkg == null || s.end <= s.start) continue;
                long from = s.start;
                while (from < s.end) {
                    long midnight = TimeUtil.nextMidnight(from);
                    long to = Math.min(s.end, midnight);
                    ContentValues v = new ContentValues();
                    v.put("pkg", s.pkg);
                    v.put("start_ts", from);
                    v.put("end_ts", to);
                    v.put("day", TimeUtil.dayKey(from));
                    db.insertWithOnConflict("sessions", null, v, SQLiteDatabase.CONFLICT_IGNORE);
                    from = to;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public synchronized void addSession(String pkg, long start, long end) {
        List<Sess> l = new ArrayList<>(1);
        l.add(new Sess(pkg, start, end));
        addSessions(l);
    }

    public synchronized void addMediaSessions(List<Sess> list) {
        if (list == null || list.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Sess s : list) {
                if (s == null || s.pkg == null || s.end <= s.start) continue;
                long from = s.start;
                while (from < s.end) {
                    long midnight = TimeUtil.nextMidnight(from);
                    long to = Math.min(s.end, midnight);
                    ContentValues v = new ContentValues();
                    v.put("pkg", s.pkg);
                    v.put("start_ts", from);
                    v.put("end_ts", to);
                    v.put("day", TimeUtil.dayKey(from));
                    db.insertWithOnConflict("media_sessions", null, v, SQLiteDatabase.CONFLICT_IGNORE);
                    from = to;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public synchronized void addMediaSession(String pkg, long start, long end) {
        List<Sess> l = new ArrayList<>(1);
        l.add(new Sess(pkg, start, end));
        addMediaSessions(l);
    }

    public synchronized long usageOnDay(String pkg, String day) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(end_ts-start_ts),0) FROM sessions WHERE day=? AND pkg=?",
                new String[]{day, pkg});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            c.close();
        }
    }

    public synchronized long dayTotal(String day) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(end_ts-start_ts),0) FROM sessions WHERE day=?", new String[]{day});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            c.close();
        }
    }

    public synchronized LinkedHashMap<String, Long> appsOnDay(String day) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT pkg, SUM(end_ts-start_ts) AS t FROM sessions WHERE day=? GROUP BY pkg ORDER BY t DESC",
                new String[]{day});
        try {
            while (c.moveToNext()) out.put(c.getString(0), c.getLong(1));
        } finally {
            c.close();
        }
        return out;
    }

    public synchronized long mediaUsageOnDay(String pkg, String day) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(end_ts-start_ts),0) FROM media_sessions WHERE day=? AND pkg=?",
                new String[]{day, pkg});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            c.close();
        }
    }

    public synchronized long mediaDayTotal(String day) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(end_ts-start_ts),0) FROM media_sessions WHERE day=?", new String[]{day});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            c.close();
        }
    }

    public synchronized LinkedHashMap<String, Long> mediaAppsOnDay(String day) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT pkg, SUM(end_ts-start_ts) AS t FROM media_sessions WHERE day=? GROUP BY pkg ORDER BY t DESC",
                new String[]{day});
        try {
            while (c.moveToNext()) out.put(c.getString(0), c.getLong(1));
        } finally {
            c.close();
        }
        return out;
    }

    public synchronized long mediaUsageBetween(String pkg, long from, long to) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(end_ts-start_ts),0) FROM media_sessions WHERE pkg=? AND start_ts>=? AND start_ts<?",
                new String[]{pkg, String.valueOf(from), String.valueOf(to)});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            c.close();
        }
    }

    public synchronized long mediaTotalBetween(long from, long to) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(end_ts-start_ts),0) FROM media_sessions WHERE start_ts>=? AND start_ts<?",
                new String[]{String.valueOf(from), String.valueOf(to)});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            c.close();
        }
    }

    public synchronized LinkedHashMap<String, Long> mediaAppsBetween(long from, long to, int limit) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT pkg, SUM(end_ts-start_ts) AS t FROM media_sessions WHERE start_ts>=? AND start_ts<? GROUP BY pkg ORDER BY t DESC LIMIT ?",
                new String[]{String.valueOf(from), String.valueOf(to), String.valueOf(limit)});
        try {
            while (c.moveToNext()) out.put(c.getString(0), c.getLong(1));
        } finally {
            c.close();
        }
        return out;
    }

    public synchronized LinkedHashMap<String, Long> mediaDailyTotals(long from, long to) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT day, SUM(end_ts-start_ts) AS t FROM media_sessions WHERE start_ts>=? AND start_ts<? GROUP BY day ORDER BY day",
                new String[]{String.valueOf(from), String.valueOf(to)});
        try {
            while (c.moveToNext()) out.put(c.getString(0), c.getLong(1));
        } finally {
            c.close();
        }
        return out;
    }

    public synchronized List<Sess> sessionsOnDay(String day) {
        List<Sess> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT pkg,start_ts,end_ts FROM sessions WHERE day=? ORDER BY start_ts", new String[]{day});
        try {
            while (c.moveToNext()) out.add(new Sess(c.getString(0), c.getLong(1), c.getLong(2)));
        } finally {
            c.close();
        }
        return out;
    }

    public synchronized long totalBetween(long from, long to) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(end_ts-start_ts),0) FROM sessions WHERE start_ts>=? AND start_ts<?",
                new String[]{String.valueOf(from), String.valueOf(to)});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            c.close();
        }
    }

    public synchronized long totalBetweenExcluding(long from, long to, String[] excluded) {
        if (excluded == null || excluded.length == 0) return totalBetween(from, to);
        String[] args = new String[2 + excluded.length];
        args[0] = String.valueOf(from);
        args[1] = String.valueOf(to);
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(SUM(end_ts-start_ts),0) FROM sessions WHERE start_ts>=? AND start_ts<? AND pkg NOT IN (");
        for (int i = 0; i < excluded.length; i++) {
            if (i > 0) sql.append(',');
            sql.append('?');
            args[2 + i] = excluded[i];
        }
        sql.append(')');
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args);
        try {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            c.close();
        }
    }

    public synchronized LinkedHashMap<String, Long> dailyTotalsExcluding(long from, long to, String[] excluded) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        if (excluded == null || excluded.length == 0) return dailyTotals(from, to);
        String[] args = new String[2 + excluded.length];
        args[0] = String.valueOf(from);
        args[1] = String.valueOf(to);
        StringBuilder sql = new StringBuilder(
                "SELECT day, SUM(end_ts-start_ts) AS t FROM sessions WHERE start_ts>=? AND start_ts<? AND pkg NOT IN (");
        for (int i = 0; i < excluded.length; i++) {
            if (i > 0) sql.append(',');
            sql.append('?');
            args[2 + i] = excluded[i];
        }
        sql.append(") GROUP BY day ORDER BY day");
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args);
        try {
            while (c.moveToNext()) out.put(c.getString(0), c.getLong(1));
        } finally {
            c.close();
        }
        return out;
    }

    public synchronized long usageBetween(String pkg, long from, long to) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(end_ts-start_ts),0) FROM sessions WHERE pkg=? AND start_ts>=? AND start_ts<?",
                new String[]{pkg, String.valueOf(from), String.valueOf(to)});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            c.close();
        }
    }

    public synchronized LinkedHashMap<String, Long> appsBetween(long from, long to, int limit) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT pkg, SUM(end_ts-start_ts) AS t FROM sessions WHERE start_ts>=? AND start_ts<? GROUP BY pkg ORDER BY t DESC LIMIT ?",
                new String[]{String.valueOf(from), String.valueOf(to), String.valueOf(limit)});
        try {
            while (c.moveToNext()) out.put(c.getString(0), c.getLong(1));
        } finally {
            c.close();
        }
        return out;
    }

    public synchronized LinkedHashMap<String, Long> dailyTotals(long from, long to) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT day, SUM(end_ts-start_ts) AS t FROM sessions WHERE start_ts>=? AND start_ts<? GROUP BY day ORDER BY day",
                new String[]{String.valueOf(from), String.valueOf(to)});
        try {
            while (c.moveToNext()) out.put(c.getString(0), c.getLong(1));
        } finally {
            c.close();
        }
        return out;
    }

    public synchronized void logBlock(String pkg) {
        ContentValues v = new ContentValues();
        long now = System.currentTimeMillis();
        v.put("pkg", pkg);
        v.put("ts", now);
        v.put("day", TimeUtil.dayKey(now));
        getWritableDatabase().insert("blocks", null, v);
    }

    public synchronized int blocksOnDay(String day) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM blocks WHERE day=?", new String[]{day});
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    public synchronized void setRule(String pkg, int limitMin, boolean enabled) {
        ContentValues v = new ContentValues();
        v.put("pkg", pkg);
        v.put("limit_min", limitMin);
        v.put("enabled", enabled ? 1 : 0);
        v.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("rules", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized void setRuleEnabled(String pkg, boolean enabled) {
        ContentValues v = new ContentValues();
        v.put("enabled", enabled ? 1 : 0);
        getWritableDatabase().update("rules", v, "pkg=?", new String[]{pkg});
    }

    public synchronized void deleteRule(String pkg) {
        getWritableDatabase().delete("rules", "pkg=?", new String[]{pkg});
    }

    public synchronized Rule rule(String pkg) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT pkg,limit_min,enabled FROM rules WHERE pkg=?", new String[]{pkg});
        try {
            if (c.moveToFirst()) {
                Rule r = new Rule();
                r.pkg = c.getString(0);
                r.limitMin = c.getInt(1);
                r.enabled = c.getInt(2) != 0;
                return r;
            }
        } finally {
            c.close();
        }
        return null;
    }

    public synchronized List<Rule> rules() {
        List<Rule> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT pkg,limit_min,enabled FROM rules ORDER BY created_at DESC", null);
        try {
            while (c.moveToNext()) {
                Rule r = new Rule();
                r.pkg = c.getString(0);
                r.limitMin = c.getInt(1);
                r.enabled = c.getInt(2) != 0;
                out.add(r);
            }
        } finally {
            c.close();
        }
        return out;
    }

    public synchronized String aiGet(String key) {
        Cursor c = getReadableDatabase().rawQuery("SELECT text FROM ai WHERE period_key=?", new String[]{key});
        try {
            return c.moveToFirst() ? c.getString(0) : null;
        } finally {
            c.close();
        }
    }

    public synchronized long aiTs(String key) {
        Cursor c = getReadableDatabase().rawQuery("SELECT ts FROM ai WHERE period_key=?", new String[]{key});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        } finally {
            c.close();
        }
    }

    public synchronized void aiPut(String key, String text) {
        ContentValues v = new ContentValues();
        v.put("period_key", key);
        v.put("text", text);
        v.put("ts", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("ai", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized void clearAi() {
        getWritableDatabase().delete("ai", null, null);
    }

    public synchronized void clearBlocks() {
        getWritableDatabase().delete("blocks", null, null);
    }

    public synchronized void clearStats() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("sessions", null, null);
        db.delete("media_sessions", null, null);
        db.delete("blocks", null, null);
        db.delete("ai", null, null);
    }

    public synchronized void prune() {
        long cutoff = TimeUtil.addDays(System.currentTimeMillis(), -400);
        getWritableDatabase().delete("sessions", "start_ts<?", new String[]{String.valueOf(cutoff)});
        getWritableDatabase().delete("media_sessions", "start_ts<?", new String[]{String.valueOf(cutoff)});
        long blockCutoff = TimeUtil.addDays(System.currentTimeMillis(), -120);
        getWritableDatabase().delete("blocks", "ts<?", new String[]{String.valueOf(blockCutoff)});
    }
}
