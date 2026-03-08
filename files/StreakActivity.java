package com.example.streakapp;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class StreakActivity extends AppCompatActivity {

    // ── GitHub green contribution levels ──────────────────────────────────────
    private static final int[] GREEN_LEVELS = {
        0xFF161B22,  // 0 – no activity
        0xFF0E4429,  // 1 – low
        0xFF006D32,  // 2 – medium
        0xFF26A641,  // 3 – high
        0xFF39D353   // 4 – max
    };

    private static final String[] DAYS_SHORT   = {"S","M","T","W","T","F","S"};
    private static final String[] MONTH_NAMES  = {
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    };

    // ── Sample activity data: month (1-based) → day → intensity (0-4) ─────────
    private final Map<String, Integer> activityData = new HashMap<String, Integer>() {{
        put("2026-3-1",  1); put("2026-3-2",  2); put("2026-3-3",  4);
        put("2026-3-4",  3); put("2026-3-5",  4); put("2026-3-6",  2);
        put("2026-3-7",  1); put("2026-3-10", 3); put("2026-3-11", 4);
        put("2026-3-12", 4); put("2026-3-13", 2); put("2026-3-14", 1);
        put("2026-3-18", 1); put("2026-3-19", 3); put("2026-3-20", 4);
        put("2026-3-24", 2); put("2026-3-25", 1);
    }};

    // ── Mini contribution strip data ──────────────────────────────────────────
    private static final int[] STRIP_LEVELS = {4,3,2,1,0,0,1,2,3,4,3,2,1,2,3,4,3,1,0,1};

    // ── State ──────────────────────────────────────────────────────────────────
    private int currentYear  = 2026;
    private int currentMonth = 3;      // 1-based (March)
    private final int todayDay = 7;    // highlight "today"

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView     tvMonthYear;
    private GridLayout   calendarGrid;
    private LinearLayout contributionStrip;
    private LinearLayout legendStrip;

    // ──────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streak);

        // Bind views
        tvMonthYear        = findViewById(R.id.tvMonthYear);
        calendarGrid       = findViewById(R.id.calendarGrid);
        contributionStrip  = findViewById(R.id.contributionStrip);
        legendStrip        = findViewById(R.id.legendStrip);

        // Close button
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

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

        // Build static parts
        buildDayHeaders();
        buildContributionStrip();
        buildLegend();

        // Build calendar for current month
        renderCalendar();

        // Entrance animations
        playEntranceAnimations();
    }

    // ── Calendar ──────────────────────────────────────────────────────────────

    /** Fills the 7-column grid with day cells for the current month/year. */
    private void renderCalendar() {
        tvMonthYear.setText(MONTH_NAMES[currentMonth - 1] + " " + currentYear);
        calendarGrid.removeAllViews();

        Calendar cal = Calendar.getInstance();
        cal.set(currentYear, currentMonth - 1, 1);

        int firstDow     = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday
        int daysInMonth  = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int totalCells   = firstDow + daysInMonth;
        int rows         = (int) Math.ceil(totalCells / 7.0);

        calendarGrid.setRowCount(rows);
        calendarGrid.setColumnCount(7);

        int cellSize = dpToPx(36);
        int gap      = dpToPx(4);

        // Empty leading cells
        for (int i = 0; i < firstDow; i++) {
            calendarGrid.addView(makeEmptyCell(cellSize, gap));
        }

        // Day cells
        for (int day = 1; day <= daysInMonth; day++) {
            String key    = currentYear + "-" + currentMonth + "-" + day;
            int    level  = activityData.containsKey(key) ? activityData.get(key) : 0;
            boolean isToday = (currentYear == 2026 && currentMonth == 3 && day == todayDay);
            calendarGrid.addView(makeDayCell(day, level, isToday, cellSize, gap));
        }
    }

    /** Creates an invisible placeholder cell. */
    private View makeEmptyCell(int size, int gap) {
        View v = new View(this);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width  = size;
        lp.height = size;
        lp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
        v.setLayoutParams(lp);
        return v;
    }

    /** Creates a single day cell with the correct GitHub-green shade. */
    private View makeDayCell(int day, int level, boolean isToday, int size, int gap) {
        TextView tv = new TextView(this);
        tv.setText(String.valueOf(day));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(10f);

        // Text colour by intensity
        if (level >= 3) {
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        } else if (level >= 1) {
            tv.setTextColor(0xFFB4F5C4);
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            tv.setTextColor(0xFF484F58);
        }

        // Background shape
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(6));

        if (isToday) {
            bg.setColor(0xFF161B22);
            bg.setStroke(dpToPx(2), 0xFF39D353);
        } else if (level > 0) {
            bg.setColor(GREEN_LEVELS[level]);
            bg.setStroke(dpToPx(1), 0x14FFFFFF);
        } else {
            bg.setColor(GREEN_LEVELS[0]);
            bg.setStroke(dpToPx(1), 0xFF21262D);
        }
        tv.setBackground(bg);

        // Layout params
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width  = size;
        lp.height = size;
        lp.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
        tv.setLayoutParams(lp);

        // Tap tooltip
        final int finalLevel = level;
        tv.setOnClickListener(v -> {
            String msg = finalLevel > 0
                ? (finalLevel * 2) + " contributions on " + MONTH_NAMES[currentMonth - 1] + " " + day
                : "No activity on " + MONTH_NAMES[currentMonth - 1] + " " + day;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Press scale animation
        tv.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(80).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                v.performClick();
            }
            return true;
        });

        return tv;
    }

    // ── Static builders ───────────────────────────────────────────────────────

    /** Builds the S M T W T F S header row. */
    private void buildDayHeaders() {
        GridLayout headers = findViewById(R.id.dayHeaders);
        headers.setColumnCount(7);
        int cellSize = dpToPx(36);
        int gap      = dpToPx(4);

        for (String label : DAYS_SHORT) {
            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setTextColor(0xFF484F58);
            tv.setTextSize(10f);
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width  = cellSize;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.setMargins(gap / 2, 0, gap / 2, 0);
            tv.setLayoutParams(lp);
            headers.addView(tv);
        }
    }

    /** Builds the small contribution strip inside the hero card. */
    private void buildContributionStrip() {
        int size = dpToPx(10);
        int gap  = dpToPx(3);

        for (int lvl : STRIP_LEVELS) {
            View cell = new View(this);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dpToPx(2));
            bg.setColor(GREEN_LEVELS[lvl]);
            bg.setStroke(dpToPx(1), 0x06FFFFFF);
            cell.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(gap / 2, 0, gap / 2, 0);
            cell.setLayoutParams(lp);
            contributionStrip.addView(cell);
        }
    }

    /** Builds the Less ■■■■■ More legend. */
    private void buildLegend() {
        int size = dpToPx(11);
        int gap  = dpToPx(5);

        for (int color : GREEN_LEVELS) {
            View cell = new View(this);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dpToPx(3));
            bg.setColor(color);
            bg.setStroke(dpToPx(1), 0x08FFFFFF);
            cell.setBackground(bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(gap / 2, 0, gap / 2, 0);
            cell.setLayoutParams(lp);
            legendStrip.addView(cell);
        }
    }

    // ── Animations ────────────────────────────────────────────────────────────

    private void playEntranceAnimations() {
        View header   = findViewById(R.id.header);
        View heroCard = findViewById(R.id.heroCard);

        // Header slides up
        header.setAlpha(0f);
        header.setTranslationY(20f);
        header.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(0).start();

        // Hero card slides up with delay
        heroCard.setAlpha(0f);
        heroCard.setTranslationY(20f);
        heroCard.animate().alpha(1f).translationY(0f).setDuration(450).setStartDelay(150).start();

        // Streak number pop
        TextView tvNum = findViewById(R.id.tvStreakNumber);
        tvNum.setScaleX(0.6f);
        tvNum.setScaleY(0.6f);
        tvNum.setAlpha(0f);
        tvNum.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(600)
            .setStartDelay(300)
            .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
            .start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
