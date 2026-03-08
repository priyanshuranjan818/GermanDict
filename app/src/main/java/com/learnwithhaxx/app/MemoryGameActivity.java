package com.learnwithhaxx.app;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MemoryGameActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private GridLayout topGrid, bottomGrid;
    private TextView timerText, scoreText, finalStats;
    private View successOverlay;
    private TextToSpeech tts;

    private List<Word> hardWords;
    private List<Word> topRowWords;
    private List<Word> bottomRowWords;

    private View selectedTopView = null;
    private View selectedBottomView = null;
    private Word selectedTopWord = null;
    private Word selectedBottomWord = null;

    private int matchesFound = 0;
    private int TOTAL_PAIRS = 9;
    private int secondsElapsed = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_game);

        db = DatabaseHelper.getInstance(this);

        topGrid = findViewById(R.id.topGrid);
        bottomGrid = findViewById(R.id.bottomGrid);
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);
        finalStats = findViewById(R.id.finalStats);
        successOverlay = findViewById(R.id.successOverlay);

        findViewById(R.id.closeBtn).setOnClickListener(v -> finish());
        findViewById(R.id.playAgainBtn).setOnClickListener(v -> startNewGame());

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.GERMAN);
            }
        });

        startNewGame();
    }

    private void startNewGame() {
        successOverlay.setVisibility(View.GONE);
        matchesFound = 0;
        secondsElapsed = 0;
        updateScoreUI();
        loadHardWords();
        
        if (hardWords.size() < TOTAL_PAIRS) {
            Toast.makeText(this, "Need at least " + TOTAL_PAIRS + " 'HARD' words to play!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Collections.shuffle(hardWords);
        List<Word> sessionWords = hardWords.subList(0, TOTAL_PAIRS);

        topRowWords = new ArrayList<>(sessionWords);
        bottomRowWords = new ArrayList<>(sessionWords);
        Collections.shuffle(topRowWords);
        Collections.shuffle(bottomRowWords);

        renderGrids();
        startTimer();
    }

    private void loadHardWords() {
        hardWords = new ArrayList<>();
        SQLiteDatabase readDb = db.getReadableDatabase();
        Cursor c = readDb.rawQuery("SELECT * FROM words WHERE level = 1", null);
        while (c.moveToNext()) {
            Word w = new Word();
            w.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            w.setGermanWord(c.getString(c.getColumnIndexOrThrow("german_word")));
            w.setMeaning(c.getString(c.getColumnIndexOrThrow("meaning")));
            hardWords.add(w);
        }
        c.close();
    }

    private void renderGrids() {
        topGrid.removeAllViews();
        bottomGrid.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < TOTAL_PAIRS; i++) {
            // Top Grid (German)
            View card = inflater.inflate(R.layout.item_memory_card, topGrid, false);
            TextView frontText = card.findViewById(R.id.frontText);
            frontText.setText(topRowWords.get(i).getGermanWord());
            card.setTag(topRowWords.get(i));
            card.setOnClickListener(v -> onTopCardClicked(v));
            topGrid.addView(card);

            // Bottom Grid (English)
            View cardB = inflater.inflate(R.layout.item_memory_card, bottomGrid, false);
            TextView frontTextB = cardB.findViewById(R.id.frontText);
            frontTextB.setText(bottomRowWords.get(i).getMeaning());
            cardB.setTag(bottomRowWords.get(i));
            cardB.setOnClickListener(v -> onBottomCardClicked(v));
            bottomGrid.addView(cardB);
        }
    }

    private void speakGerman(String word) {
        if (tts != null) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "german_word");
        }
    }

    private void onTopCardClicked(View v) {
        if (v == selectedTopView || v.getAlpha() < 1f) return;

        if (selectedTopView != null) {
            flipCard(selectedTopView, false);
        }

        selectedTopView = v;
        selectedTopWord = (Word) v.getTag();
        setCardBackground(v, R.drawable.bg_memory_card_selected);
        flipCard(v, true);

        speakGerman(selectedTopWord.getGermanWord());

        checkMatch();
    }

    private void onBottomCardClicked(View v) {
        if (v == selectedBottomView || v.getAlpha() < 1f) return;

        if (selectedBottomView != null) {
            flipCard(selectedBottomView, false);
        }

        selectedBottomView = v;
        selectedBottomWord = (Word) v.getTag();
        setCardBackground(v, R.drawable.bg_memory_card_selected);
        flipCard(v, true);

        checkMatch();
    }

    private void checkMatch() {
        if (selectedTopWord != null && selectedBottomWord != null) {
            final View vTop = selectedTopView;
            final View vBottom = selectedBottomView;

            if (selectedTopWord.getId() == selectedBottomWord.getId()) {
                // MATCH!
                setCardBackground(vTop, R.drawable.bg_memory_card_correct);
                setCardBackground(vBottom, R.drawable.bg_memory_card_correct);

                new Handler().postDelayed(() -> {
                    vTop.animate().alpha(0.5f).setDuration(300);
                    vBottom.animate().alpha(0.5f).setDuration(300);
                    vTop.setClickable(false);
                    vBottom.setClickable(false);
                }, 600);

                matchesFound++;
                updateScoreUI();
                resetSelection();

                if (matchesFound == TOTAL_PAIRS) {
                    endGame();
                }
            } else {
                // WRONG
                setCardBackground(vTop, R.drawable.bg_memory_card_wrong);
                setCardBackground(vBottom, R.drawable.bg_memory_card_wrong);

                resetSelection();

                new Handler().postDelayed(() -> {
                    flipCard(vTop, false);
                    flipCard(vBottom, false);
                }, 1000);
            }
        }
    }

    private void resetSelection() {
        selectedTopView = null;
        selectedBottomView = null;
        selectedTopWord = null;
        selectedBottomWord = null;
    }

    private void setCardBackground(View view, int drawableRes) {
        View front = view.findViewById(R.id.cardFront);
        if (front != null) {
            front.setBackground(ContextCompat.getDrawable(this, drawableRes));
        }
    }

    private void flipCard(View view, boolean showFront) {
        View back = view.findViewById(R.id.cardBack);
        View front = view.findViewById(R.id.cardFront);

        float end = showFront ? 180f : 0f;

        view.animate().rotationY(end).setDuration(300).start();
        
        new Handler().postDelayed(() -> {
            if (showFront) {
                back.setVisibility(View.GONE);
                front.setVisibility(View.VISIBLE);
                front.setRotationY(180); 
            } else {
                back.setVisibility(View.VISIBLE);
                front.setVisibility(View.GONE);
                // Reset background to white when flipped back
                front.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
            }
        }, 150);
    }

    private void startTimer() {
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                secondsElapsed++;
                int mins = secondsElapsed / 60;
                int secs = secondsElapsed % 60;
                timerText.setText(String.format("%02d:%02d", mins, secs));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateScoreUI() {
        scoreText.setText("Score: " + matchesFound + " / " + TOTAL_PAIRS);
    }

    private void endGame() {
        timerHandler.removeCallbacks(timerRunnable);
        successOverlay.setVisibility(View.VISIBLE);
        
        String stars = "⭐⭐⭐";
        if (secondsElapsed > 60) stars = "⭐⭐";
        if (secondsElapsed > 100) stars = "⭐";
        
        finalStats.setText("Time: " + timerText.getText() + " | Stars: " + stars);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        timerHandler.removeCallbacks(timerRunnable);
        super.onDestroy();
    }
}
