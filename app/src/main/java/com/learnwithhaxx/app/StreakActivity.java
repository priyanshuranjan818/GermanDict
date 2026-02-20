package com.learnwithhaxx.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StreakActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private List<String> streakDates;
    private Map<String, Integer> streakCounts;
    private Calendar currentCal = Calendar.getInstance();

    // Views
    private TextView streakNumber;
    private GridLayout githubStreakGrid;
    private TextView monthTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streak);

        db = DatabaseHelper.getInstance(this);

        streakNumber = findViewById(R.id.streakNumber);
        githubStreakGrid = findViewById(R.id.githubStreakGrid);
        monthTitle = findViewById(R.id.monthTitle);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.calPrev).setOnClickListener(v -> {
            currentCal.add(Calendar.MONTH, -1);
            renderGithubStreak();
        });
        findViewById(R.id.calNext).setOnClickListener(v -> {
            currentCal.add(Calendar.MONTH, 1);
            renderGithubStreak();
        });

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        User user = db.getUser();
        streakDates = db.getStreakDates();
        streakCounts = db.getStreakCounts();
        streakNumber.setText(String.valueOf(user.getStreak()));

        renderGithubStreak();
    }

    private void renderGithubStreak() {
        githubStreakGrid.removeAllViews();

        int currentMonth = currentCal.get(Calendar.MONTH);
        int currentYear = currentCal.get(Calendar.YEAR);

        // Start of month
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(currentYear, currentMonth, 1);
        // Clear time fields to avoid comparison issues
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);

        // Calculate the first Sunday to display
        Calendar gridStart = (Calendar) monthStart.clone();
        int dayOfWeek = gridStart.get(Calendar.DAY_OF_WEEK);
        // Calendar.SUNDAY is 1. Move back to the previous Sunday.
        gridStart.add(Calendar.DAY_OF_YEAR, -(dayOfWeek - 1));

        githubStreakGrid.setColumnCount(7);
        githubStreakGrid.setRowCount(6);

        int cellSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        int radius = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat monthSdf = new SimpleDateFormat("MMMM yyyy", Locale.US);

        monthTitle.setText(monthSdf.format(monthStart.getTime()));

        Calendar iterCal = (Calendar) gridStart.clone();
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                View cellContainer = getLayoutInflater().inflate(R.layout.item_calendar_day, githubStreakGrid, false);
                TextView dayText = cellContainer.findViewById(R.id.dayText);
                View dayBg = cellContainer.findViewById(R.id.dayBg);

                String dateStr = sdf.format(iterCal.getTime());

                if (iterCal.get(Calendar.MONTH) == currentMonth) {
                    dayText.setText(String.valueOf(iterCal.get(Calendar.DAY_OF_MONTH)));
                    int count = streakCounts != null && streakCounts.containsKey(dateStr) ? streakCounts.get(dateStr) : 0;
                    
                    int levelColor = getLevelColorFromCount(dateStr);
                    
                    GradientDrawable shape = new GradientDrawable();
                    shape.setShape(GradientDrawable.RECTANGLE);
                    shape.setCornerRadius(radius);
                    shape.setColor(levelColor);
                    dayBg.setBackground(shape);

                    if (count > 0) {
                        dayText.setTextColor(Color.WHITE);
                    } else {
                        dayText.setTextColor(ContextCompat.getColor(this, R.color.text_main));
                    }
                } else {
                    dayText.setText("");
                    dayBg.setBackgroundColor(Color.TRANSPARENT);
                }

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(margin, margin, margin, margin);
                params.columnSpec = GridLayout.spec(c, 1f);
                params.rowSpec = GridLayout.spec(r);
                cellContainer.setLayoutParams(params);

                githubStreakGrid.addView(cellContainer);
                iterCal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
    }

    private int getLevelColorFromCount(String dateStr) {
        Integer count = streakCounts != null ? streakCounts.get(dateStr) : null;
        if (count == null || count == 0) {
            return ContextCompat.getColor(this, R.color.git_level_0);
        } else if (count < 3) {
            return ContextCompat.getColor(this, R.color.git_level_1);
        } else if (count < 6) {
            return ContextCompat.getColor(this, R.color.git_level_2);
        } else if (count < 10) {
            return ContextCompat.getColor(this, R.color.git_level_3);
        } else {
            return ContextCompat.getColor(this, R.color.git_level_4);
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
            }
            return id == R.id.nav_streak;
        });
    }
}
