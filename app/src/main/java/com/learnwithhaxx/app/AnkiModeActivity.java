package com.learnwithhaxx.app;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AnkiModeActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextToSpeech tts;
    private List<Word> words;
    private int currentIndex = 0;

    private TextView cardGerman, cardMeaning, cardExample, progressText;
    private View divider;
    private Button btnShowAnswer;
    private View ratingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anki_mode);

        db = DatabaseHelper.getInstance(this);

        cardGerman = findViewById(R.id.cardGerman);
        cardMeaning = findViewById(R.id.cardMeaning);
        cardExample = findViewById(R.id.cardExample);
        progressText = findViewById(R.id.progressText);
        divider = findViewById(R.id.divider);
        btnShowAnswer = findViewById(R.id.btnShowAnswer);
        ratingLayout = findViewById(R.id.ratingLayout);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.GERMAN);
            }
        });

        loadWords();

        btnShowAnswer.setOnClickListener(v -> revealAnswer());
        
        // HARD button (btnAgain in XML) saves as Level 1
        findViewById(R.id.btnAgain).setOnClickListener(v -> updateAndNext(1));
        // EASY button saves as Level 2
        findViewById(R.id.btnEasy).setOnClickListener(v -> updateAndNext(2));
    }

    private void loadWords() {
        // Fetch words based on the 100 limit
        // Logic in DatabaseHelper will prioritize Level 0 (New), then 80/20 mix of Level 1 (Hard) and 2 (Easy)
        words = db.getWordsForPractice(100);
        
        if (words.isEmpty()) {
            Toast.makeText(this, "No words to practice!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Collections.shuffle(words);
        showCard();
    }

    private void showCard() {
        if (currentIndex >= words.size()) {
            Toast.makeText(this, "Session complete!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Word word = words.get(currentIndex);
        
        // Coloring German Article
        String german = word.getGermanWord();
        String lower = german.toLowerCase();
        if (lower.startsWith("der ") || lower.startsWith("die ") || lower.startsWith("das ")) {
            SpannableString ss = new SpannableString(german);
            int color;
            if (lower.startsWith("der ")) color = ContextCompat.getColor(this, R.color.blue_primary);
            else if (lower.startsWith("die ")) color = ContextCompat.getColor(this, R.color.red_primary);
            else color = ContextCompat.getColor(this, R.color.green_primary);
            ss.setSpan(new ForegroundColorSpan(color), 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            cardGerman.setText(ss);
        } else {
            cardGerman.setText(german);
        }

        cardMeaning.setText(word.getMeaning());
        cardExample.setText(word.getExample() != null ? word.getExample() : "");

        // Hide answer elements
        divider.setVisibility(View.INVISIBLE);
        cardMeaning.setVisibility(View.INVISIBLE);
        cardExample.setVisibility(View.INVISIBLE);
        
        ratingLayout.setVisibility(View.GONE);
        btnShowAnswer.setVisibility(View.VISIBLE);

        progressText.setText((currentIndex + 1) + " / " + words.size());
        
        speakGerman(word.getGermanWord());
    }

    private void revealAnswer() {
        divider.setVisibility(View.VISIBLE);
        cardMeaning.setVisibility(View.VISIBLE);
        cardExample.setVisibility(View.VISIBLE);
        
        btnShowAnswer.setVisibility(View.GONE);
        ratingLayout.setVisibility(View.VISIBLE);
    }

    private void updateAndNext(int level) {
        Word currentWord = words.get(currentIndex);
        db.updateWordLevel(currentWord.getId(), level);

        currentIndex++;
        showCard();
    }

    private void speakGerman(String word) {
        if (tts != null) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "anki_pronunciation");
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
