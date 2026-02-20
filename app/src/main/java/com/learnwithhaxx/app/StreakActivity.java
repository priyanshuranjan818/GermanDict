package com.learnwithhaxx.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StreakActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private Calendar calendarDate;
    private List<String> streakDates;

    // Views
    private TextView streakNumber;
    private TextView motivationText;
    private TextView calMonth;
    private GridLayout calendarGrid;
    private ProgressBar goalProgress;
    private TextView goalCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streak);

        db = DatabaseHelper.getInstance(this);
        calendarDate = Calendar.getInstance();

        // Views
        streakNumber = findViewById(R.id.streakNumber);
        motivationText = findViewById(R.id.motivationText);
        calMonth = findViewById(R.id.calMonth);
        calendarGrid = findViewById(R.id.calendarGrid);
        goalProgress = findViewById(R.id.goalProgress);
        goalCountText = findViewById(R.id.goalCountText);

        // Back button
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // Start Lesson button
        findViewById(R.id.startLessonBtn).setOnClickListener(v -> {
            startActivity(new Intent(this, AddWordActivity.class));
        });

        // Calendar navigation
        findViewById(R.id.calPrev).setOnClickListener(v -> changeMonth(-1));
        findViewById(R.id.calNext).setOnClickListener(v -> changeMonth(1));

        // Bottom Navigation
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        User user = db.getUser();
        int todayCount = db.getTodayWordCount();
        streakDates = db.getStreakDates();

        // Streak hero
        streakNumber.setText(String.valueOf(user.getStreak()));

        // Motivation text
        int remaining = Math.max(5 - todayCount, 0);
        String wordText = remaining == 1 ? "word" : "words";
        motivationText.setText("Add " + remaining + " more " + wordText + " today to extend your streak!");

        // Goal progress
        goalProgress.setMax(5);
        goalProgress.setProgress(Math.min(todayCount, 5));
        goalCountText.setText(todayCount + "/5");

        // Render calendar
        renderCalendar();
    }

    private void changeMonth(int delta) {
        calendarDate.add(Calendar.MONTH, delta);
        renderCalendar();
    }

    private void renderCalendar() {
        int year = calendarDate.get(Calendar.YEAR);
        int month = calendarDate.get(Calendar.MONTH);

        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        calMonth.setText(months[month] + " " + year);

        // First day of month (0=Sunday)
        Calendar first = Calendar.getInstance();
        first.set(year, month, 1);
        int firstDayOfWeek = first.get(Calendar.DAY_OF_WEEK) - 1; // 0-based

        // Days in month
        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Today string
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());

        calendarGrid.removeAllViews();

        int totalCells = firstDayOfWeek + daysInMonth;
        int rows = (int) Math.ceil(totalCells / 7.0);
        calendarGrid.setRowCount(rows + 1); // +1 for safety

        int cellSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics());

        // Empty cells before first day
        for (int i = 0; i < firstDayOfWeek; i++) {
            TextView empty = new TextView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = cellSize;
            params.columnSpec = GridLayout.spec(i % 7, 1f);
            params.rowSpec = GridLayout.spec(i / 7);
            empty.setLayoutParams(params);
            calendarGrid.addView(empty);
        }

        // Day cells
        for (int d = 1; d <= daysInMonth; d++) {
            TextView dayView = new TextView(this);
            dayView.setText(String.valueOf(d));
            dayView.setGravity(Gravity.CENTER);
            dayView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            dayView.setTypeface(null, Typeface.BOLD);
            dayView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            int cellIndex = firstDayOfWeek + d - 1;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = cellSize;
            params.columnSpec = GridLayout.spec(cellIndex % 7, 1f);
            params.rowSpec = GridLayout.spec(cellIndex / 7);
            dayView.setLayoutParams(params);

            String dateStr = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, d);

            if (streakDates != null && streakDates.contains(dateStr)) {
                dayView.setBackgroundResource(R.drawable.bg_cal_active);
                dayView.setTextColor(Color.WHITE);
            }

            if (dateStr.equals(todayStr)) {
                if (streakDates != null && streakDates.contains(dateStr)) {
                    // Active + today: keep active background
                } else {
                    dayView.setBackgroundResource(R.drawable.bg_cal_today);
                }
            }

            calendarGrid.addView(dayView);
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_streak);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddWordActivity.class));
                return true;
            } else if (id == R.id.nav_learn) {
                startActivity(new Intent(this, LearnActivity.class));
                return true;
            } else if (id == R.id.nav_streak) {
                return true;
            }
            return false;
        });
    }
}
