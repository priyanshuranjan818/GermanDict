package com.learnwithhaxx.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PracticeSelectionActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextView statLevel0, statLevel1, statLevel2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_selection);

        db = DatabaseHelper.getInstance(this);

        statLevel0 = findViewById(R.id.statLevel0);
        statLevel1 = findViewById(R.id.statLevel1);
        statLevel2 = findViewById(R.id.statLevel2);

        findViewById(R.id.btnMemoryGame).setOnClickListener(v -> {
            startActivity(new Intent(this, MemoryGameActivity.class));
        });

        findViewById(R.id.btnMatchGame).setOnClickListener(v -> {
            startActivity(new Intent(this, MatchWordsActivity.class));
        });

        findViewById(R.id.btnAnkiMode).setOnClickListener(v -> {
            startActivity(new Intent(this, AnkiModeActivity.class));
        });

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
    }

    private void updateStats() {
        statLevel0.setText(getString(R.string.anki_new, db.getWordCountByLevel(0)));
        statLevel1.setText(getString(R.string.anki_hard, db.getWordCountByLevel(1)));
        statLevel2.setText(getString(R.string.anki_easy, db.getWordCountByLevel(2)));
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_practice);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddWordActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_nouns) {
                startActivity(new Intent(this, NounsActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_practice) {
                return true;
            } else if (id == R.id.nav_streak) {
                startActivity(new Intent(this, StreakActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
