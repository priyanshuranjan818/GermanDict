package com.learnwithhaxx.app;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextToSpeech tts;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView streakCount;
    private TextView todayCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = DatabaseHelper.getInstance(this);

        recyclerView = findViewById(R.id.wordRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        streakCount = findViewById(R.id.streakCount);
        todayCount = findViewById(R.id.todayCount);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Streak badge click → navigate to streak page
        findViewById(R.id.streakBadge).setOnClickListener(v -> {
            startActivity(new Intent(this, StreakActivity.class));
        });

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.GERMAN);
            }
        });

        // Bottom Navigation
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        // Load stats
        User user = db.getUser();
        streakCount.setText(String.valueOf(user.getStreak()));
        int count = db.getTodayWordCount();
        todayCount.setText(count + " / 5");

        // Load words grouped by category
        Map<String, List<Word>> grouped = db.getWordsByCategory();

        if (grouped.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);

            // Build flat list with category headers
            List<Object> items = new ArrayList<>();
            for (Map.Entry<String, List<Word>> entry : grouped.entrySet()) {
                items.add(entry.getKey() + " (" + entry.getValue().size() + ")");
                items.addAll(entry.getValue());
            }

            recyclerView.setAdapter(new WordAdapter(items, word -> speakGerman(word)));
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
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddWordActivity.class));
                return true;
            } else if (id == R.id.nav_learn) {
                startActivity(new Intent(this, LearnActivity.class));
                return true;
            } else if (id == R.id.nav_streak) {
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

    // ─── RecyclerView Adapter ────────────────────────────

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_WORD = 1;

    public interface WordClickListener {
        void onSpeakClick(String word);
    }

    private static class WordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Object> items;
        private final WordClickListener listener;

        WordAdapter(List<Object> items, WordClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof String ? TYPE_HEADER : TYPE_WORD;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_category_header, parent, false);
                return new HeaderVH(v);
            } else {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_word, parent, false);
                return new WordVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).title.setText((String) items.get(position));
            } else if (holder instanceof WordVH) {
                Word word = (Word) items.get(position);
                WordVH wh = (WordVH) holder;
                wh.germanWord.setText(word.getGermanWord());
                wh.meaning.setText(word.getMeaning());
                wh.speakBtn.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSpeakClick(word.getGermanWord());
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class HeaderVH extends RecyclerView.ViewHolder {
            TextView title;
            HeaderVH(View v) {
                super(v);
                title = v.findViewById(R.id.categoryTitle);
            }
        }

        static class WordVH extends RecyclerView.ViewHolder {
            TextView germanWord, meaning;
            View speakBtn;
            WordVH(View v) {
                super(v);
                germanWord = v.findViewById(R.id.germanWord);
                meaning = v.findViewById(R.id.wordMeaning);
                speakBtn = v.findViewById(R.id.speakBtn);
            }
        }
    }
}
