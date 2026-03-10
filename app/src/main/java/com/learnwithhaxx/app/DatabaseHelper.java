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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "vocab.db";
    private static final int DATABASE_VERSION = 3;

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
                "level INTEGER DEFAULT 0, " +
                "FOREIGN KEY (user_id) REFERENCES users(id))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STREAK_DATES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "active_date TEXT NOT NULL, " +
                "word_count INTEGER DEFAULT 0, " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "UNIQUE(user_id, active_date))");

        ContentValues cv = new ContentValues();
        cv.put("name", "Learner");
        cv.put("streak", 0);
        db.insert(TABLE_USERS, null, cv);

        seedDefaultWords(db);
    }

    private void seedDefaultWords(SQLiteDatabase db) {
        String today = getToday();
        insertWord(db, "der Apfel", "Apple", "Ich esse einen Apfel.", "Nomen", today);
        insertWord(db, "die Lampe", "Lamp", "Die Lampe ist hell.", "Nomen", today);
        insertWord(db, "das Haus", "House", "Das Haus ist groß.", "Nomen", today);
        insertWord(db, "laufen", "to run", "Ich laufe schnell.", "Verb", today);
        insertWord(db, "schön", "beautiful", "Das Wetter ist schön.", "Adjektiv", today);
    }

    private void insertWord(SQLiteDatabase db, String german, String meaning, String example, String pos, String date) {
        ContentValues cv = new ContentValues();
        cv.put("user_id", 1);
        cv.put("german_word", german);
        cv.put("meaning", meaning);
        cv.put("example", example);
        cv.put("part_of_speech", pos);
        cv.put("date_added", date);
        cv.put("level", 0);
        db.insert(TABLE_WORDS, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_STREAK_DATES + " ADD COLUMN word_count INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_WORDS + " ADD COLUMN level INTEGER DEFAULT 0");
        }
    }

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

        // Check if streak is broken
        String today = getToday();
        String yesterday = getYesterday();
        String lastActive = user.getLastActiveDate();

        if (lastActive != null && !lastActive.equals(today) && !lastActive.equals(yesterday)) {
            user.setStreak(0);
            SQLiteDatabase wdb = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("streak", 0);
            wdb.update(TABLE_USERS, cv, "id = 1", null);
        }

        return user;
    }

    public void updateWordLevel(int wordId, int level) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("level", level);
        db.update(TABLE_WORDS, cv, "id = ?", new String[]{String.valueOf(wordId)});
    }

    public int getWordCountByLevel(int level) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORDS + " WHERE level = ?", new String[]{String.valueOf(level)});
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public List<Word> getWordsForPractice(int limit) {
        List<Word> practiceWords = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        // 1. First Priority: Get NEW words (level 0)
        Cursor cNew = db.rawQuery("SELECT * FROM " + TABLE_WORDS + " WHERE level = 0 ORDER BY RANDOM() LIMIT " + limit, null);
        if (cNew.getCount() > 0) {
            while (cNew.moveToNext()) {
                practiceWords.add(cursorToWord(cNew));
            }
            cNew.close();
            return practiceWords; // Only show new words until they are all gone
        }
        cNew.close();

        // 2. Second Priority (Review Phase): 80% Hard (1) and 20% Easy (2)
        int hardLimit = (int) (limit * 0.8);
        int easyLimit = limit - hardLimit;

        Cursor cHard = db.rawQuery("SELECT * FROM " + TABLE_WORDS + " WHERE level = 1 ORDER BY RANDOM() LIMIT " + hardLimit, null);
        while (cHard.moveToNext()) {
            practiceWords.add(cursorToWord(cHard));
        }
        cHard.close();

        Cursor cEasy = db.rawQuery("SELECT * FROM " + TABLE_WORDS + " WHERE level = 2 ORDER BY RANDOM() LIMIT " + easyLimit, null);
        while (cEasy.moveToNext()) {
            practiceWords.add(cursorToWord(cEasy));
        }
        cEasy.close();

        // If limits didn't fill up (e.g. not enough Easy words), fill with any categorized words
        if (practiceWords.size() < limit) {
            Cursor cRemaining = db.rawQuery("SELECT * FROM " + TABLE_WORDS + " WHERE level > 0 ORDER BY RANDOM() LIMIT " + (limit - practiceWords.size()), null);
            while (cRemaining.moveToNext()) {
                Word w = cursorToWord(cRemaining);
                boolean exists = false;
                for (Word existing : practiceWords) {
                    if (existing.getId() == w.getId()) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) practiceWords.add(w);
            }
            cRemaining.close();
        }

        return practiceWords;
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
        cv.put("level", 0);
        return db.insert(TABLE_WORDS, null, cv);
    }

    public List<Word> getAllWords() {
        List<Word> words = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_WORDS + " WHERE user_id = 1 ORDER BY id DESC", null);
        while (c.moveToNext()) {
            words.add(cursorToWord(c));
        }
        c.close();
        return words;
    }

    public Map<String, List<Word>> getWordsByCategory() {
        Map<String, List<Word>> grouped = new LinkedHashMap<>();
        List<Word> allWords = getAllWords();
        for (Word word : allWords) {
            String category = word.getPartOfSpeech();
            if (category == null || category.isEmpty()) {
                category = "Uncategorized";
            }
            if (!grouped.containsKey(category)) {
                grouped.put(category, new ArrayList<>());
            }
            grouped.get(category).add(word);
        }
        return grouped;
    }

    public boolean isDuplicateWord(String germanWord) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM " + TABLE_WORDS + " WHERE german_word = ? COLLATE NOCASE AND user_id = 1", new String[]{germanWord});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
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
        w.setLevel(c.getInt(c.getColumnIndexOrThrow("level")));
        return w;
    }

    public void updateStreak() {
        SQLiteDatabase db = getWritableDatabase();
        String today = getToday();
        String yesterday = getYesterday();

        db.execSQL("INSERT OR IGNORE INTO " + TABLE_STREAK_DATES + " (user_id, active_date, word_count) VALUES (1, ?, 0)", new Object[]{today});
        db.execSQL("UPDATE " + TABLE_STREAK_DATES + " SET word_count = word_count + 1 WHERE user_id = 1 AND active_date = ?", new Object[]{today});

        // Check if goal reached (at least 5 words today)
        int todayCount = getTodayWordCount();
        if (todayCount >= 5) {
            User user = getUser();
            int currentStreak = user.getStreak();
            String lastActive = user.getLastActiveDate();

            if (today.equals(lastActive)) {
                // Goal already reached and streak updated today
            } else if (yesterday.equals(lastActive)) {
                // Streak continued
                currentStreak++;
                db.execSQL("UPDATE " + TABLE_USERS + " SET streak = ?, last_active_date = ? WHERE id = 1", new Object[]{currentStreak, today});
            } else {
                // New streak started
                currentStreak = 1;
                db.execSQL("UPDATE " + TABLE_USERS + " SET streak = ?, last_active_date = ? WHERE id = 1", new Object[]{currentStreak, today});
            }
        }
    }

    public List<String> getStreakDates() {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT active_date FROM " + TABLE_STREAK_DATES + " WHERE user_id = 1 ORDER BY active_date DESC", null);
        while (c.moveToNext()) {
            dates.add(c.getString(0));
        }
        c.close();
        return dates;
    }

    public Map<String, Integer> getStreakCounts() {
        Map<String, Integer> counts = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT active_date, word_count FROM " + TABLE_STREAK_DATES + " WHERE user_id = 1", null);
        while (c.moveToNext()) {
            counts.put(c.getString(0), c.getInt(1));
        }
        c.close();
        return counts;
    }

    private String getToday() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
    private String getYesterday() { Calendar cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -1); return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime()); }
    public void deleteWord(int wordId) { getWritableDatabase().delete(TABLE_WORDS, "id = ?", new String[]{String.valueOf(wordId)}); }
    public int getTodayWordCount() { SQLiteDatabase db = getReadableDatabase(); Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORDS + " WHERE date_added = ?", new String[]{getToday()}); int count = 0; if (c.moveToFirst()) count = c.getInt(0); c.close(); return count; }
}
