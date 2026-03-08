package com.learnwithhaxx.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PracticeSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_selection);

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

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_practice);
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
                return true;
            } else if (id == R.id.nav_streak) {
                startActivity(new Intent(this, StreakActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
