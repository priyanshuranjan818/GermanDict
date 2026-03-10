package com.learnwithhaxx.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NounsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextToSpeech tts;
    private RecyclerView nounRecyclerView;
    private LinearLayout emptyNouns;
    private TextView emptyNounTitle;
    private LinearLayout btnDer, btnDie, btnDas;
    
    private List<Word> allNouns = new ArrayList<>();
    private List<Word> filteredNouns = new ArrayList<>();
    private NounAdapter adapter;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nouns);

        db = DatabaseHelper.getInstance(this);

        nounRecyclerView = findViewById(R.id.nounRecyclerView);
        emptyNouns = findViewById(R.id.emptyNouns);
        emptyNounTitle = findViewById(R.id.emptyNounTitle);
        
        btnDer = findViewById(R.id.btnDer);
        btnDie = findViewById(R.id.btnDie);
        btnDas = findViewById(R.id.btnDas);

        nounRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.GERMAN);
            }
        });

        btnDer.setOnClickListener(v -> toggleFilter("der"));
        btnDie.setOnClickListener(v -> toggleFilter("die"));
        btnDas.setOnClickListener(v -> toggleFilter("das"));

        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNouns();
    }

    private void loadNouns() {
        List<Word> words = db.getAllWords();
        allNouns.clear();
        
        for (Word word : words) {
            String lower = word.getGermanWord().toLowerCase();
            String cat = word.getPartOfSpeech();
            if (lower.startsWith("der ") || lower.startsWith("die ") || lower.startsWith("das ") || 
                (cat != null && (cat.equalsIgnoreCase("Nomen") || cat.equalsIgnoreCase("Noun")))) {
                allNouns.add(word);
            }
        }
        
        applyFilter(currentFilter);
    }

    private void toggleFilter(String filter) {
        if (currentFilter.equals(filter)) {
            applyFilter("all");
        } else {
            applyFilter(filter);
        }
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filteredNouns.clear();
        
        // Update UI: if "all", all buttons look active. If specific, only that one.
        if (filter.equals("all")) {
            btnDer.setAlpha(1.0f);
            btnDie.setAlpha(1.0f);
            btnDas.setAlpha(1.0f);
            filteredNouns.addAll(allNouns);
        } else {
            btnDer.setAlpha(filter.equals("der") ? 1.0f : 0.5f);
            btnDie.setAlpha(filter.equals("die") ? 1.0f : 0.5f);
            btnDas.setAlpha(filter.equals("das") ? 1.0f : 0.5f);

            for (Word word : allNouns) {
                if (word.getGermanWord().toLowerCase().startsWith(filter + " ")) {
                    filteredNouns.add(word);
                }
            }
        }

        if (filteredNouns.isEmpty()) {
            nounRecyclerView.setVisibility(View.GONE);
            emptyNouns.setVisibility(View.VISIBLE);
            if (filter.equals("all")) {
                emptyNounTitle.setText("No nouns found");
            } else {
                emptyNounTitle.setText("No '" + filter + "' nouns found");
            }
        } else {
            nounRecyclerView.setVisibility(View.VISIBLE);
            emptyNouns.setVisibility(View.GONE);
            if (adapter == null) {
                adapter = new NounAdapter(filteredNouns);
                nounRecyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(filteredNouns);
            }
        }
    }

    private void speakGerman(String word) {
        if (tts != null) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "noun_pronunciation");
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_nouns);
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
                // Add to Nouns is left slide
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_nouns) {
                applyFilter("all");
                return true;
            } else if (id == R.id.nav_practice) {
                startActivity(new Intent(this, PracticeSelectionActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private class NounAdapter extends RecyclerView.Adapter<NounAdapter.NounViewHolder> {
        private List<Word> words;

        public NounAdapter(List<Word> words) {
            this.words = words;
        }

        public void updateList(List<Word> newList) {
            this.words = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public NounViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_word, parent, false);
            return new NounViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull NounViewHolder holder, int position) {
            Word word = words.get(position);
            String german = word.getGermanWord();
            String lower = german.toLowerCase();

            if (lower.startsWith("der ") || lower.startsWith("die ") || lower.startsWith("das ")) {
                SpannableString ss = new SpannableString(german);
                int color;
                int end = 3; // "der", "die", "das" are all 3 chars
                
                if (lower.startsWith("der ")) {
                    color = ContextCompat.getColor(NounsActivity.this, R.color.blue_primary);
                } else if (lower.startsWith("die ")) {
                    color = ContextCompat.getColor(NounsActivity.this, R.color.red_primary);
                } else {
                    color = ContextCompat.getColor(NounsActivity.this, R.color.green_primary);
                }
                
                ss.setSpan(new ForegroundColorSpan(color), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.germanWord.setText(ss);
            } else {
                holder.germanWord.setText(german);
            }

            holder.meaning.setText(word.getMeaning());
            
            holder.speakBtn.setOnClickListener(v -> speakGerman(word.getGermanWord()));
            
            holder.deleteBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(NounsActivity.this)
                        .setTitle("Delete Word")
                        .setMessage("Delete \"" + word.getGermanWord() + "\"?")
                        .setPositiveButton("Delete", (d, w) -> {
                            db.deleteWord(word.getId());
                            loadNouns();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return words.size();
        }

        class NounViewHolder extends RecyclerView.ViewHolder {
            TextView germanWord, meaning;
            ImageButton speakBtn, deleteBtn;

            public NounViewHolder(@NonNull View v) {
                super(v);
                germanWord = v.findViewById(R.id.germanWord);
                meaning = v.findViewById(R.id.wordMeaning);
                speakBtn = v.findViewById(R.id.speakBtn);
                deleteBtn = v.findViewById(R.id.deleteBtn);
            }
        }
    }
}
