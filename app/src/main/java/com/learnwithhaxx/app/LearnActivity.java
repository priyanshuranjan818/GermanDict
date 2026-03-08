package com.learnwithhaxx.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LearnActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextToSpeech ttsGerman;
    private TextToSpeech ttsEnglish;

    private List<Word> words;
    private int currentIndex = 0;
    private boolean isPaused = false;
    private boolean shouldShuffle = true;

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
        shouldShuffle = getIntent().getBooleanExtra("shuffle", true);

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
        ttsGerman = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsGerman.setLanguage(Locale.GERMAN);
                ttsEnglish = new TextToSpeech(this, status2 -> {
                    if (status2 == TextToSpeech.SUCCESS) {
                        ttsEnglish.setLanguage(Locale.US);
                        // Ensure UI updates happen on the Main Thread
                        runOnUiThread(this::initSlideshow);
                    }
                });
            }
        });

        autoAdvanceHandler = new Handler(Looper.getMainLooper());
        setupBottomNav();
    }

    private void initSlideshow() {
        words = db.getAllWords();

        if (words.isEmpty()) {
            slideWord.setText(R.string.no_words_learn);
            slideMeaning.setText(R.string.no_words_learn_sub);
            controlsContainer.setVisibility(View.GONE);
            return;
        }

        if (shouldShuffle) {
            Collections.shuffle(words);
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

        // Animation logic
        AnimationSet animationSet = new AnimationSet(true);
        
        float fromY = shouldShuffle ? 60f : -100f;
        Animation slide = new TranslateAnimation(0, 0, fromY, 0);
        slide.setDuration(400);
        
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(400);

        animationSet.addAnimation(slide);
        animationSet.addAnimation(fadeIn);

        slideWord.startAnimation(animationSet);
        slideMeaning.startAnimation(animationSet);
        if (slideExample.getVisibility() == View.VISIBLE) {
            slideExample.startAnimation(animationSet);
        }

        String german = word.getGermanWord();
        String lower = german.toLowerCase();

        if (lower.startsWith("der ") || lower.startsWith("die ") || lower.startsWith("das ")) {
            SpannableString ss = new SpannableString(german);
            int color;
            int end = 3; 
            
            if (lower.startsWith("der ")) {
                color = ContextCompat.getColor(this, R.color.blue_primary);
            } else if (lower.startsWith("die ")) {
                color = ContextCompat.getColor(this, R.color.red_primary);
            } else {
                color = ContextCompat.getColor(this, R.color.green_primary);
            }
            
            ss.setSpan(new ForegroundColorSpan(color), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            slideWord.setText(ss);
        } else {
            slideWord.setText(german);
        }

        slideMeaning.setText(word.getMeaning());
        slideExample.setText(word.getExample() != null ? word.getExample() : "");

        updateProgressBar();
        updateProgressText();

        // Auto-pronounce after short delay
        autoAdvanceHandler.postDelayed(() -> {
            speakGerman(word.getGermanWord());
            autoAdvanceHandler.postDelayed(() -> speakEnglish(word.getMeaning()), 1000);
        }, 300);
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

        if (words != null) {
            progressBar.setProgress(words.size());
            progressText.setText(words.size() + " / " + words.size());
        }
    }

    private void speakGerman(String word) {
        if (ttsGerman != null) {
            ttsGerman.speak(word, TextToSpeech.QUEUE_FLUSH, null, "learn_word");
        }
    }

    private void speakEnglish(String word) {
        if (ttsEnglish != null) {
            ttsEnglish.speak(word, TextToSpeech.QUEUE_FLUSH, null, "learn_meaning");
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        bottomNav.getMenu().setGroupCheckable(0, true, false); 
        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            bottomNav.getMenu().getItem(i).setChecked(false);
        }
        bottomNav.getMenu().setGroupCheckable(0, true, true);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddWordActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_nouns) {
                startActivity(new Intent(this, NounsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_practice) {
                startActivity(new Intent(this, MatchWordsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_streak) {
                startActivity(new Intent(this, StreakActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        stopAutoAdvance();
        if (ttsGerman != null) {
            ttsGerman.stop();
            ttsGerman.shutdown();
        }
        if (ttsEnglish != null) {
            ttsEnglish.stop();
            ttsEnglish.shutdown();
        }
        super.onDestroy();
    }
}
