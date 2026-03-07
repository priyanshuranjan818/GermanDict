package com.learnwithhaxx.app;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextToSpeech tts;
    private RecyclerView wordRecyclerView;
    private LinearLayout emptyState;
    private TextView streakCount;
    private TextView todayCount;
    private TextView totalWordCount;
    private SearchView searchView;
    private TextView headerTitle;
    
    private WordAdapter adapter;
    private List<Word> wordList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = DatabaseHelper.getInstance(this);

        wordRecyclerView = findViewById(R.id.wordRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        streakCount = findViewById(R.id.streakCount);
        todayCount = findViewById(R.id.todayCount);
        totalWordCount = findViewById(R.id.totalWordCount);
        searchView = findViewById(R.id.searchView);
        headerTitle = findViewById(R.id.headerTitle);

        wordRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.streakBadge).setOnClickListener(v -> {
            startActivity(new Intent(this, StreakActivity.class));
        });

        findViewById(R.id.learnRecentBtn).setOnClickListener(v -> {
            Intent intent = new Intent(this, LearnActivity.class);
            intent.putExtra("shuffle", false);
            startActivity(intent);
        });

        // The Total Words badge now acts as the Search trigger
        findViewById(R.id.totalWordsSearchBtn).setOnClickListener(v -> {
            if (searchView.getVisibility() == View.VISIBLE) {
                closeSearch();
            } else {
                openSearch();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterWords(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterWords(newText);
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            closeSearch();
            return false;
        });

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.GERMAN);
            }
        });

        setupBottomNav();
    }

    private void openSearch() {
        searchView.setVisibility(View.VISIBLE);
        headerTitle.setVisibility(View.GONE);
        searchView.requestFocus();
        searchView.setIconified(false);
    }

    private void closeSearch() {
        searchView.setVisibility(View.GONE);
        headerTitle.setVisibility(View.VISIBLE);
        searchView.setQuery("", false);
        if (adapter != null) {
            adapter.updateList(wordList);
        }
    }

    private void filterWords(String query) {
        if (query.isEmpty()) {
            if (adapter != null) adapter.updateList(wordList);
            return;
        }
        
        List<Word> filteredList = new ArrayList<>();
        for (Word word : wordList) {
            if (word.getGermanWord().toLowerCase().contains(query.toLowerCase()) ||
                word.getMeaning().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(word);
            }
        }
        if (adapter != null) {
            adapter.updateList(filteredList);
        }
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

        wordList = db.getAllWords();
        totalWordCount.setText(String.valueOf(wordList.size()));

        if (wordList.isEmpty()) {
            wordRecyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            wordRecyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            adapter = new WordAdapter(wordList);
            wordRecyclerView.setAdapter(adapter);
        }
        
        // Refresh search if active
        if (searchView.getVisibility() == View.VISIBLE) {
            filterWords(searchView.getQuery().toString());
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
            if (id == R.id.nav_practice) {
                startActivity(new Intent(this, MatchWordsActivity.class));
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

    private class WordAdapter extends RecyclerView.Adapter<WordAdapter.WordViewHolder> {
        private List<Word> words;

        public WordAdapter(List<Word> words) {
            this.words = words;
        }

        public void updateList(List<Word> newList) {
            this.words = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_word, parent, false);
            return new WordViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
            Word word = words.get(position);
            holder.germanWord.setText(word.getGermanWord());
            holder.meaning.setText(word.getMeaning());
            
            holder.speakBtn.setOnClickListener(v -> speakGerman(word.getGermanWord()));
            
            holder.deleteBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Word")
                        .setMessage("Delete \"" + word.getGermanWord() + "\"?")
                        .setPositiveButton("Delete", (d, w) -> {
                            db.deleteWord(word.getId());
                            loadData();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return words.size();
        }

        class WordViewHolder extends RecyclerView.ViewHolder {
            TextView germanWord, meaning;
            ImageButton speakBtn, deleteBtn;

            public WordViewHolder(@NonNull View v) {
                super(v);
                germanWord = v.findViewById(R.id.germanWord);
                meaning = v.findViewById(R.id.wordMeaning);
                speakBtn = v.findViewById(R.id.speakBtn);
                deleteBtn = v.findViewById(R.id.deleteBtn);
            }
        }
    }
}
