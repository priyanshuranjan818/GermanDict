package com.learnwithhaxx.app;

import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MatchWordsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextToSpeech tts;
    
    private LinearLayout leftColumn, rightColumn;
    private ProgressBar progressBar;
    
    private List<Word> currentWords;
    private List<Word> shuffledGerman;
    private List<Word> shuffledEnglish;
    
    private View selectedGermanView = null;
    private View selectedEnglishView = null;
    private Word selectedGermanWord = null;
    private Word selectedEnglishWord = null;
    
    private int matchesFound = 0;
    private static final int TOTAL_MATCHES = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_words);

        db = DatabaseHelper.getInstance(this);
        
        leftColumn = findViewById(R.id.leftColumn);
        rightColumn = findViewById(R.id.rightColumn);
        progressBar = findViewById(R.id.matchProgressBar);
        
        // Remove success overlay references if they exist in layout but we don't want to use it
        View successOverlay = findViewById(R.id.successOverlay);
        if (successOverlay != null) successOverlay.setVisibility(View.GONE);
        
        findViewById(R.id.closeBtn).setOnClickListener(v -> finish());
        
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.GERMAN);
            }
        });

        loadNewRound();
    }

    private void loadNewRound() {
        List<Word> allWords = db.getAllWords();
        if (allWords.size() < TOTAL_MATCHES) {
            Toast.makeText(this, "Add at least " + TOTAL_MATCHES + " words to practice!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Collections.shuffle(allWords);
        currentWords = allWords.subList(0, TOTAL_MATCHES);
        
        shuffledGerman = new ArrayList<>(currentWords);
        shuffledEnglish = new ArrayList<>(currentWords);
        Collections.shuffle(shuffledGerman);
        Collections.shuffle(shuffledEnglish);

        renderColumns();
    }

    private void renderColumns() {
        leftColumn.removeAllViews();
        rightColumn.removeAllViews();
        matchesFound = 0;
        progressBar.setProgress(0);
        progressBar.setMax(TOTAL_MATCHES);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Word w : shuffledGerman) {
            View v = inflater.inflate(R.layout.item_match_card, leftColumn, false);
            TextView tv = v.findViewById(R.id.cardText);
            tv.setText(w.getGermanWord());
            v.setTag(w);
            v.setOnClickListener(this::onGermanClick);
            leftColumn.addView(v);
        }

        for (Word w : shuffledEnglish) {
            View v = inflater.inflate(R.layout.item_match_card, rightColumn, false);
            TextView tv = v.findViewById(R.id.cardText);
            tv.setText(w.getMeaning());
            v.setTag(w);
            v.setOnClickListener(this::onEnglishClick);
            rightColumn.addView(v);
        }
    }

    private void onGermanClick(View v) {
        if (v.getAlpha() < 1f) return; 
        
        Word word = (Word) v.getTag();
        speakGerman(word.getGermanWord());

        if (selectedGermanView != null) {
            selectedGermanView.findViewById(R.id.cardText).setBackgroundResource(R.drawable.bg_rounded_card);
        }

        selectedGermanView = v;
        selectedGermanWord = word;
        v.findViewById(R.id.cardText).setBackgroundResource(R.drawable.bg_match_card_selected);

        checkMatch();
    }

    private void onEnglishClick(View v) {
        if (v.getAlpha() < 1f) return; 

        if (selectedEnglishView != null) {
            selectedEnglishView.findViewById(R.id.cardText).setBackgroundResource(R.drawable.bg_rounded_card);
        }

        selectedEnglishView = v;
        selectedEnglishWord = (Word) v.getTag();
        v.findViewById(R.id.cardText).setBackgroundResource(R.drawable.bg_match_card_selected);

        checkMatch();
    }

    private void checkMatch() {
        if (selectedGermanWord != null && selectedEnglishWord != null) {
            if (selectedGermanWord.getId() == selectedEnglishWord.getId()) {
                handleCorrectMatch(selectedGermanView, selectedEnglishView);
            } else {
                handleWrongMatch(selectedGermanView, selectedEnglishView);
            }
            selectedGermanWord = null;
            selectedEnglishWord = null;
            selectedGermanView = null;
            selectedEnglishView = null;
        }
    }

    private void handleCorrectMatch(View v1, View v2) {
        v1.findViewById(R.id.cardText).setBackgroundResource(R.drawable.bg_match_card_correct);
        v2.findViewById(R.id.cardText).setBackgroundResource(R.drawable.bg_match_card_correct);
        
        matchesFound++;
        progressBar.setProgress(matchesFound);

        new Handler().postDelayed(() -> {
            v1.setAlpha(0.3f);
            v2.setAlpha(0.3f);
            v1.setClickable(false);
            v2.setClickable(false);
            if (matchesFound == TOTAL_MATCHES) {
                // Automatically load new round after a short delay
                new Handler().postDelayed(this::loadNewRound, 500);
            }
        }, 500);
    }

    private void handleWrongMatch(View v1, View v2) {
        v1.findViewById(R.id.cardText).setBackgroundResource(R.drawable.bg_match_card_wrong);
        v2.findViewById(R.id.cardText).setBackgroundResource(R.drawable.bg_match_card_wrong);

        new Handler().postDelayed(() -> {
            v1.findViewById(R.id.cardText).setBackgroundResource(R.drawable.bg_rounded_card);
            v2.findViewById(R.id.cardText).setBackgroundResource(R.drawable.bg_rounded_card);
        }, 500);
    }

    private void speakGerman(String word) {
        if (tts != null) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "match_pronunciation");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
