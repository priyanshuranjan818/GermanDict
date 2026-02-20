package com.learnwithhaxx.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

public class LearnActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextToSpeech tts;

    private List<Word> words;
    private int currentIndex = 0;
    private boolean isPaused = false;

    private Handler autoAdvanceHandler;
    private Runnable autoAdvanceRunnable;

    // Views
    private ProgressBar progressBar;
    private TextView progressText;
    private LinearLayout slideshowContainer;
    private LinearLayout sessionComplete;
    private LinearLayout controlsContainer;
    private TextView slideWord, slideMeaning, slideExample;
    private ImageButton prevBtn, pauseBtn, nextBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);

        db = DatabaseHelper.getInstance(this);

        // Views
        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        slideshowContainer = findViewById(R.id.slideshowContainer);
        sessionComplete = findViewById(R.id.sessionComplete);
        controlsContainer = findViewById(R.id.controlsContainer);
        slideWord = findViewById(R.id.slideWord);
        slideMeaning = findViewById(R.id.slideMeaning);
        slideExample = findViewById(R.id.slideExample);
        prevBtn = findViewById(R.id.prevBtn);
        pauseBtn = findViewById(R.id.pauseBtn);
        nextBtn = findViewById(R.id.nextBtn);

        // Back button
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // Back to dashboard button in session complete
        findViewById(R.id.backToDashboardBtn).setOnClickListener(v -> finish());

        // Controls
        prevBtn.setOnClickListener(v -> prevSlide());
        pauseBtn.setOnClickListener(v -> togglePause());
        nextBtn.setOnClickListener(v -> nextSlide());

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.GERMAN);
                // Load words after TTS is ready
                initSlideshow();
            }
        });

        autoAdvanceHandler = new Handler(Looper.getMainLooper());
    }

    private void initSlideshow() {
        words = db.getAllWords();

        if (words.isEmpty()) {
            slideWord.setText(R.string.no_words_learn);
            slideMeaning.setText(R.string.no_words_learn_sub);
            controlsContainer.setVisibility(View.GONE);
            return;
        }

        progressBar.setMax(words.size());
        updateProgressText();
        showCurrentWord();
        startAutoAdvance();
    }

    private void showCurrentWord() {
        if (currentIndex >= words.size()) {
            showSessionComplete();
            return;
        }

        Word word = words.get(currentIndex);

        // Animate slide in
        Animation slideIn = new TranslateAnimation(0, 0, 60, 0);
        slideIn.setDuration(300);
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(300);

        slideWord.startAnimation(fadeIn);
        slideMeaning.startAnimation(fadeIn);

        slideWord.setText(word.getGermanWord());
        slideMeaning.setText(word.getMeaning());
        slideExample.setText(word.getExample() != null ? word.getExample() : "");

        updateProgressBar();
        updateProgressText();

        // Auto-pronounce after short delay
        autoAdvanceHandler.postDelayed(() -> speakGerman(word.getGermanWord()), 300);
    }

    private void updateProgressBar() {
        progressBar.setProgress(currentIndex + 1);
    }

    private void updateProgressText() {
        if (words == null || words.isEmpty()) {
            progressText.setText("0 / 0");
        } else {
            progressText.setText((currentIndex + 1) + " / " + words.size());
        }
    }

    private void startAutoAdvance() {
        stopAutoAdvance();
        autoAdvanceRunnable = () -> {
            if (!isPaused && currentIndex < words.size()) {
                currentIndex++;
                showCurrentWord();
                startAutoAdvance();
            }
        };
        autoAdvanceHandler.postDelayed(autoAdvanceRunnable, 4000);
    }

    private void stopAutoAdvance() {
        if (autoAdvanceRunnable != null) {
            autoAdvanceHandler.removeCallbacks(autoAdvanceRunnable);
        }
    }

    private void nextSlide() {
        currentIndex++;
        if (currentIndex >= words.size()) {
            showSessionComplete();
        } else {
            showCurrentWord();
        }
        startAutoAdvance();
    }

    private void prevSlide() {
        if (currentIndex > 0) {
            currentIndex--;
            showCurrentWord();
            startAutoAdvance();
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            pauseBtn.setImageResource(android.R.drawable.ic_media_play);
            stopAutoAdvance();
        } else {
            pauseBtn.setImageResource(android.R.drawable.ic_media_pause);
            startAutoAdvance();
        }
    }

    private void showSessionComplete() {
        stopAutoAdvance();
        slideshowContainer.setVisibility(View.GONE);
        controlsContainer.setVisibility(View.GONE);
        sessionComplete.setVisibility(View.VISIBLE);

        // Fill progress to 100%
        if (words != null) {
            progressBar.setProgress(words.size());
            progressText.setText(words.size() + " / " + words.size());
        }
    }

    private void speakGerman(String word) {
        if (tts != null) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "learn_word");
        }
    }

    @Override
    protected void onDestroy() {
        stopAutoAdvance();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
