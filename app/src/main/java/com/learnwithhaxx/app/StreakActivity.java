package com.learnwithhaxx.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class StreakActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private Map<String, Integer> streakCounts;

    // ── GitHub green contribution levels (Reversed: Light to Dark) ─────────────
    private static final int[] GREEN_LEVELS = {
        0xFFEBEDF0,  // 0 – no activity (Light Gray for light theme)
        0xFF9BE9A8,  // 1 – low
        0xFF40C463,  // 2 – medium
        0xFF30A14E,  // 3 – high
        0xFF216E39   // 4 – max
    };

    private static final String[] DAYS_SHORT   = {"S","M","T","W","T","F","S"};
    private static final String[] MONTH_NAMES  = {
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    };

    // ── State ──────────────────────────────────────────────────────────────────
    private int currentYear;
    private int currentMonth; // 1-based
    private String todayStr;

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView     tvMonthYear;
    private GridLayout   calendarGrid;
    private LinearLayout contributionStrip;
    private LinearLayout legendStrip;
    private TextView     tvStreakNumber;
    private TextView     tvBestVal;
    private TextView     tvActiveVal;
    private TextView     tvTotalVal;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streak);

        db = DatabaseHelper.getInstance(this);
        
        Calendar now = Calendar.getInstance();
        currentYear = now.get(Calendar.YEAR);
        currentMonth = now.get(Calendar.MONTH) + 1;
        todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.getTime());

        // Bind views
        tvMonthYear        = findViewById(R.id.tvMonthYear);
        calendarGrid       = findViewById(R.id.calendarGrid);
        contributionStrip  = findViewById(R.id.contributionStrip);
        legendStrip        = findViewById(R.id.legendStrip);
        tvStreakNumber     = findViewById(R.id.tvStreakNumber);
        tvBestVal          = findViewById(R.id.tvBestVal);
        tvActiveVal        = findViewById(R.id.tvActiveVal);
        tvTotalVal         = findViewById(R.id.tvTotalVal);
        bottomNav          = findViewById(R.id.bottomNav);

        // Month navigation
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 1) { currentMonth = 12; currentYear--; }
            renderCalendar();
        });
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 12) { currentMonth = 1; currentYear++; }
            renderCalendar();
        });

        setupBottomNav();
        buildDayHeaders();
        buildLegend();
        
        loadData();
        renderCalendar();
        playEntranceAnimations();
    }

    private void loadData() {
        User user = db.getUser();
        streakCounts = db.getStreakCounts();
        
        tvStreakNumber.setText(String.valueOf(user.getStreak()));
        tvBestVal.setText(user.getStreak() + "d");
        
        int totalWordsCount = db.getAllWords().size();
        tvTotalVal.setText(totalWordsCount + "w");

        int activeCount = 0;
        if (streakCounts != null) {
            for (int count : streakCounts.values()) {
                if (count > 0) activeCount++;
            }
        }
        tvActiveVal.setText(activeCount + "d");

        renderContributionStrip();
    }

    private void renderCalendar() {
        tvMonthYear.setText(MONTH_NAMES[currentMonth - 1] + " " + currentYear);
        calendarGrid.removeAllViews();

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth - 1, 1);

        int firstDow     = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday
        int daysInMonth  = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        calendarGrid.setColumnCount(7);

        int cellSize = dpToPx(36);
        int gap      = dpToPx(4);

        // Empty leading cells
        for (int i = 0; i < firstDow; i++) {
            calendarGrid.addView(makeEmptyCell(cellSize, gap));
        }

        // Day cells
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = sdf.format(cal.getTime());
            
            int count = streakCounts != null && streakCounts.containsKey(dateStr) ? streakCounts.get(dateStr) : 0;
            int level = getLevelFromCount(count);
            
            boolean isToday = dateStr.equals(todayStr);
            calendarGrid.addView(makeDayCell(day, level, isToday, cellSize, gap, count));
        }

        // Fill trailing empty cells to maintain a constant grid height (always 6 rows = 42 cells)
        int totalAdded = firstDow + daysInMonth;
        for (int i = totalAdded; i < 42; i++) {
            calendarGrid.addView(makeEmptyCell(cellSize, gap));
        }
    }

    private int getLevelFromCount(int count) {
        if (count == 0) return 0;
        if (count < 3) return 1;
        if (count < 6) return 2;
        if (count < 10) return 3;
        return 4;
    }

    private View makeEmptyCell(int size, int gap) {
        View v = new View(this);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width  = size;
        lp.height = size;
        lp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
        v.setLayoutParams(lp);
        return v;
    }

    private View makeDayCell(int day, int level, boolean isToday, int size, int gap, int count) {
        TextView tv = new TextView(this);
        tv.setText(String.valueOf(day));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(12f);
        tv.setIncludeFontPadding(false);
        tv.setPadding(0, 0, 0, 0);

        // ALWAYS BLACK TEXT for light theme date numbers
        tv.setTextColor(0xFF000000); 
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(6));

        if (isToday) {
            bg.setColor(Color.WHITE);
            bg.setStroke(dpToPx(2), 0xFF39D353);
        } else if (level > 0) {
            bg.setColor(GREEN_LEVELS[level]);
            bg.setStroke(dpToPx(1), 0x14000000);
        } else {
            bg.setColor(GREEN_LEVELS[0]); // Light gray background for empty cells
            bg.setStroke(dpToPx(1), 0xFFE0E0E0);
        }
        tv.setBackground(bg);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width  = size;
        lp.height = size;
        lp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
        tv.setLayoutParams(lp);

        tv.setOnClickListener(v -> {
            String msg = count > 0
                ? count + " words on " + MONTH_NAMES[currentMonth - 1] + " " + day
                : "No activity on " + MONTH_NAMES[currentMonth - 1] + " " + day;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        return tv;
    }

    private void buildDayHeaders() {
        GridLayout headers = findViewById(R.id.dayHeaders);
        headers.setColumnCount(7);
        int cellSize = dpToPx(36);
        int gap      = dpToPx(4);

        for (String label : DAYS_SHORT) {
            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setTextColor(0xFF000000); // Black day headers
            tv.setTextSize(11f);
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            tv.setIncludeFontPadding(false);
            tv.setPadding(0, 0, 0, 0);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width  = cellSize;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.setMargins(gap / 2, 0, gap / 2, 0);
            tv.setLayoutParams(lp);
            headers.addView(tv);
        }
    }

    private void renderContributionStrip() {
        contributionStrip.removeAllViews();
        int size = dpToPx(10);
        int gap  = dpToPx(3);
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -19);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (int i = 0; i < 20; i++) {
            String dateStr = sdf.format(cal.getTime());
            int count = streakCounts != null && streakCounts.containsKey(dateStr) ? streakCounts.get(dateStr) : 0;
            int level = getLevelFromCount(count);

            View cell = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dpToPx(2));
            bg.setColor(GREEN_LEVELS[level]);
            bg.setStroke(dpToPx(1), 0x14000000);
            cell.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(gap / 2, 0, gap / 2, 0);
            cell.setLayoutParams(lp);
            contributionStrip.addView(cell);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void buildLegend() {
        legendStrip.removeAllViews();
        int size = dpToPx(11);
        int gap  = dpToPx(5);

        for (int color : GREEN_LEVELS) {
            View cell = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dpToPx(3));
            bg.setColor(color);
            bg.setStroke(dpToPx(1), 0x14000000);
            cell.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(gap / 2, 0, gap / 2, 0);
            cell.setLayoutParams(lp);
            legendStrip.addView(cell);
        }
    }

    private void playEntranceAnimations() {
        View header   = findViewById(R.id.header);
        View heroCard = findViewById(R.id.heroCardWrapper);

        header.setAlpha(0f);
        header.setTranslationY(20f);
        header.animate().alpha(1f).translationY(0f).setDuration(400).start();

        heroCard.setAlpha(0f);
        heroCard.setTranslationY(20f);
        heroCard.animate().alpha(1f).translationY(0f).setDuration(450).setStartDelay(150).start();

        tvStreakNumber.setScaleX(0.6f);
        tvStreakNumber.setScaleY(0.6f);
        tvStreakNumber.setAlpha(0f);
        tvStreakNumber.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(600)
            .setStartDelay(300)
            .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
            .start();
    }

    private void setupBottomNav() {
        if (bottomNav == null) return;
        
        bottomNav.setSelectedItemId(R.id.nav_streak);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (itemId == R.id.nav_add) {
                startActivity(new Intent(this, AddWordActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (itemId == R.id.nav_nouns) {
                startActivity(new Intent(this, NounsActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (itemId == R.id.nav_practice) {
                startActivity(new Intent(this, PracticeSelectionActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (itemId == R.id.nav_streak) {
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

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
