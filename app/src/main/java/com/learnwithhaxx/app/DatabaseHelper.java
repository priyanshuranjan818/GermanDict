package com.learnwithhaxx.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "vocab.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_WORDS = "words";
    private static final String TABLE_STREAK_DATES = "streak_dates";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "streak INTEGER DEFAULT 0, " +
                "last_active_date TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WORDS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "german_word TEXT NOT NULL, " +
                "meaning TEXT NOT NULL, " +
                "example TEXT, " +
                "part_of_speech TEXT, " +
                "date_added TEXT NOT NULL, " +
                "FOREIGN KEY (user_id) REFERENCES users(id))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STREAK_DATES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "active_date TEXT NOT NULL, " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "UNIQUE(user_id, active_date))");

        // Seed default user
        ContentValues cv = new ContentValues();
        cv.put("name", "Learner");
        cv.put("streak", 0);
        db.insert(TABLE_USERS, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future migrations go here
    }

    // ─── Helper ─────────────────────────────────────────

    private String getToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    // ─── User ───────────────────────────────────────────

    public User getUser() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE id = 1", null);
        User user = new User();
        if (c.moveToFirst()) {
            user.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            user.setName(c.getString(c.getColumnIndexOrThrow("name")));
            user.setStreak(c.getInt(c.getColumnIndexOrThrow("streak")));
            user.setLastActiveDate(c.getString(c.getColumnIndexOrThrow("last_active_date")));
        }
        c.close();
        return user;
    }

    // ─── Words ──────────────────────────────────────────

    public boolean isDuplicateWord(String germanWord) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM " + TABLE_WORDS + " WHERE user_id = 1 AND LOWER(german_word) = LOWER(?)",
                new String[]{germanWord});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public long addWord(String germanWord, String meaning, String example, String partOfSpeech) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_id", 1);
        cv.put("german_word", germanWord);
        cv.put("meaning", meaning);
        cv.put("example", example);
        cv.put("part_of_speech", partOfSpeech);
        cv.put("date_added", getToday());
        return db.insert(TABLE_WORDS, null, cv);
    }

    public void deleteWord(int wordId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_WORDS, "id = ? AND user_id = 1", new String[]{String.valueOf(wordId)});
    }

    public List<Word> getAllWords() {
        List<Word> words = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_WORDS + " WHERE user_id = 1 ORDER BY date_added DESC", null);
        while (c.moveToNext()) {
            words.add(cursorToWord(c));
        }
        c.close();
        return words;
    }

    /**
     * Returns words grouped by part_of_speech, ordered by category then date.
     */
    public Map<String, List<Word>> getWordsByCategory() {
        Map<String, List<Word>> grouped = new LinkedHashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_WORDS + " WHERE user_id = 1 ORDER BY part_of_speech, date_added DESC",
                null);
        while (c.moveToNext()) {
            Word w = cursorToWord(c);
            String cat = w.getPartOfSpeech();
            if (cat == null || cat.isEmpty()) cat = "Uncategorized";
            if (!grouped.containsKey(cat)) {
                grouped.put(cat, new ArrayList<>());
            }
            grouped.get(cat).add(w);
        }
        c.close();
        return grouped;
    }

    public int getTodayWordCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_WORDS + " WHERE user_id = 1 AND date_added = ?",
                new String[]{getToday()});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    private Word cursorToWord(Cursor c) {
        Word w = new Word();
        w.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        w.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
        w.setGermanWord(c.getString(c.getColumnIndexOrThrow("german_word")));
        w.setMeaning(c.getString(c.getColumnIndexOrThrow("meaning")));
        w.setExample(c.getString(c.getColumnIndexOrThrow("example")));
        w.setPartOfSpeech(c.getString(c.getColumnIndexOrThrow("part_of_speech")));
        w.setDateAdded(c.getString(c.getColumnIndexOrThrow("date_added")));
        return w;
    }

    // ─── Streak ─────────────────────────────────────────

    public void updateStreak() {
        SQLiteDatabase db = getWritableDatabase();
        User user = getUser();
        String today = getToday();
        String yesterday = getYesterday();
        int todayCount = getTodayWordCount();

        String lastActive = user.getLastActiveDate();
        int currentStreak = user.getStreak();

        if (todayCount >= 5) {
            // Log today as active
            ContentValues cv = new ContentValues();
            cv.put("user_id", 1);
            cv.put("active_date", today);
            db.insertWithOnConflict(TABLE_STREAK_DATES, null, cv, SQLiteDatabase.CONFLICT_IGNORE);

            if (today.equals(lastActive)) {
                // Already updated today — do nothing
            } else if (yesterday.equals(lastActive)) {
                // Consecutive day → increment
                currentStreak += 1;
                ContentValues uv = new ContentValues();
                uv.put("streak", currentStreak);
                uv.put("last_active_date", today);
                db.update(TABLE_USERS, uv, "id = 1", null);
            } else if (lastActive == null) {
                // First time
                ContentValues uv = new ContentValues();
                uv.put("streak", 1);
                uv.put("last_active_date", today);
                db.update(TABLE_USERS, uv, "id = 1", null);
            } else {
                // Missed day(s) → reset
                ContentValues uv = new ContentValues();
                uv.put("streak", 1);
                uv.put("last_active_date", today);
                db.update(TABLE_USERS, uv, "id = 1", null);
            }
        } else {
            // Haven't hit 5 yet
            if (lastActive != null && !lastActive.equals(today) && !lastActive.equals(yesterday)) {
                // Missed day(s) → reset streak
                ContentValues uv = new ContentValues();
                uv.put("streak", 0);
                db.update(TABLE_USERS, uv, "id = 1", null);
            }
        }
    }

    public List<String> getStreakDates() {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT active_date FROM " + TABLE_STREAK_DATES + " WHERE user_id = 1", null);
        while (c.moveToNext()) {
            dates.add(c.getString(0));
        }
        c.close();
        return dates;
    }
}
