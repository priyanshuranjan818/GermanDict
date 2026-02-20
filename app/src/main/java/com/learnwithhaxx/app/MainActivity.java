package com.learnwithhaxx.app;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextToSpeech tts;
    private ExpandableListView expandableListView;
    private LinearLayout emptyState;
    private TextView streakCount;
    private TextView todayCount;
    
    private List<String> allCategoriesList = new ArrayList<>();
    private Map<String, List<Word>> allDataMap = new HashMap<>();
    private WordExpandableAdapter adapter;

    private int isolatedGroupIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = DatabaseHelper.getInstance(this);

        expandableListView = findViewById(R.id.wordExpandableList);
        emptyState = findViewById(R.id.emptyState);
        streakCount = findViewById(R.id.streakCount);
        todayCount = findViewById(R.id.todayCount);

        findViewById(R.id.streakBadge).setOnClickListener(v -> {
            startActivity(new Intent(this, StreakActivity.class));
        });

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.GERMAN);
            }
        });

        setupBottomNav();

        expandableListView.setOnGroupExpandListener(groupPosition -> {
            if (isolatedGroupIndex == -1) {
                isolatedGroupIndex = groupPosition;
                adapter.notifyDataSetChanged();
            }
        });

        expandableListView.setOnGroupCollapseListener(groupPosition -> {
            // This is handled by the back button usually, but just in case
            if (isolatedGroupIndex != -1) {
                isolatedGroupIndex = -1;
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        User user = db.getUser();
        streakCount.setText(String.valueOf(user.getStreak()));
        int count = db.getTodayWordCount();
        todayCount.setText(count + " / 5");

        Map<String, List<Word>> grouped = db.getWordsByCategory();
        String[] allCategories = getResources().getStringArray(R.array.parts_of_speech);
        
        allCategoriesList.clear();
        allDataMap.clear();

        for (int i = 1; i < allCategories.length; i++) {
            String cat = allCategories[i];
            allCategoriesList.add(cat);
            allDataMap.put(cat, grouped.getOrDefault(cat, new ArrayList<>()));
        }

        if (grouped.containsKey("Uncategorized") && !grouped.get("Uncategorized").isEmpty()) {
            allCategoriesList.add("Uncategorized");
            allDataMap.put("Uncategorized", grouped.get("Uncategorized"));
        }

        if (allDataMap.isEmpty() && db.getAllWords().isEmpty()) {
            expandableListView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            expandableListView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            adapter = new WordExpandableAdapter();
            expandableListView.setAdapter(adapter);
            
            // If we were isolated, keep it (or reset if data changed significantly)
            if (isolatedGroupIndex >= allCategoriesList.size()) {
                isolatedGroupIndex = -1;
            }
        }
    }

    private void speakGerman(String word) {
        if (tts != null) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "german_word");
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddWordActivity.class));
                return true;
            }
            if (id == R.id.nav_learn) {
                startActivity(new Intent(this, LearnActivity.class));
                return true;
            }
            if (id == R.id.nav_streak) {
                startActivity(new Intent(this, StreakActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private class WordExpandableAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return isolatedGroupIndex == -1 ? allCategoriesList.size() : 1;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            String category = getCategoryName(groupPosition);
            return allDataMap.get(category).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return getCategoryName(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            String category = getCategoryName(groupPosition);
            return allDataMap.get(category).get(childPosition);
        }

        private String getCategoryName(int groupPosition) {
            if (isolatedGroupIndex != -1) {
                return allCategoriesList.get(isolatedGroupIndex);
            }
            return allCategoriesList.get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            String category = (String) getGroup(groupPosition);
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_category_header, parent, false);
            }

            View backArrow = convertView.findViewById(R.id.backArrow);
            TextView icon = convertView.findViewById(R.id.categoryIcon);
            TextView title = convertView.findViewById(R.id.categoryTitle);
            TextView count = convertView.findViewById(R.id.categoryCount);
            TextView arrow = convertView.findViewById(R.id.categoryArrow);

            title.setText(category);
            count.setText(String.valueOf(allDataMap.get(category).size()));
            
            if (isolatedGroupIndex != -1) {
                backArrow.setVisibility(View.VISIBLE);
                arrow.setVisibility(View.GONE);
                icon.setText("📂");
                backArrow.setOnClickListener(v -> {
                    expandableListView.collapseGroup(0);
                    isolatedGroupIndex = -1;
                    notifyDataSetChanged();
                });
            } else {
                backArrow.setVisibility(View.GONE);
                arrow.setVisibility(View.VISIBLE);
                icon.setText(isExpanded ? "📂" : "📁");
                arrow.setText(isExpanded ? "▾" : "▸");
                backArrow.setOnClickListener(null);
            }

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            Word word = (Word) getChild(groupPosition, childPosition);
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_word, parent, false);
            }

            TextView germanWord = convertView.findViewById(R.id.germanWord);
            TextView meaning = convertView.findViewById(R.id.wordMeaning);
            ImageButton speakBtn = convertView.findViewById(R.id.speakBtn);

            germanWord.setText(word.getGermanWord());
            meaning.setText(word.getMeaning());
            speakBtn.setOnClickListener(v -> speakGerman(word.getGermanWord()));

            convertView.setPadding(60, 0, 0, 0);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
