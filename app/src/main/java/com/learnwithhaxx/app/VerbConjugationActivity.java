package com.learnwithhaxx.app;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VerbConjugationActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private ExpandableListView expandableListView;
    private View emptyVerbs;
    private TextView verbTotalCount;
    private List<Word> verbs = new ArrayList<>();
    private VerbExpandableAdapter adapter;
    private TextToSpeech tts;

    private int isolatedGroupIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verbs);

        db = DatabaseHelper.getInstance(this);
        expandableListView = findViewById(R.id.verbExpandableList);
        emptyVerbs = findViewById(R.id.emptyVerbs);
        verbTotalCount = findViewById(R.id.verbTotalCount);

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
        loadVerbs();
    }

    private void loadVerbs() {
        Map<String, List<Word>> grouped = db.getWordsByCategory();
        verbs.clear();
        
        for (String category : grouped.keySet()) {
            String lowerCat = category.toLowerCase();
            if (lowerCat.equals("verb") || lowerCat.startsWith("verb ")) {
                verbs.addAll(grouped.get(category));
            }
        }

        verbTotalCount.setText(String.valueOf(verbs.size()));

        if (verbs.isEmpty()) {
            expandableListView.setVisibility(View.GONE);
            emptyVerbs.setVisibility(View.VISIBLE);
        } else {
            expandableListView.setVisibility(View.VISIBLE);
            emptyVerbs.setVisibility(View.GONE);
            adapter = new VerbExpandableAdapter();
            expandableListView.setAdapter(adapter);
            
            if (isolatedGroupIndex >= verbs.size()) {
                isolatedGroupIndex = -1;
            }
        }
    }

    private void speakGerman(String word) {
        if (tts != null) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "verb_pronunciation");
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_verbs);
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
            } else if (id == R.id.nav_verbs) {
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

    private class VerbExpandableAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return isolatedGroupIndex == -1 ? verbs.size() : 1;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return 1;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return getVerb(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return getVerb(groupPosition);
        }

        private Word getVerb(int groupPosition) {
            if (isolatedGroupIndex != -1) {
                return verbs.get(isolatedGroupIndex);
            }
            return verbs.get(groupPosition);
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
            Word verb = (Word) getGroup(groupPosition);
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_category_header, parent, false);
            }

            View backArrow = convertView.findViewById(R.id.backArrow);
            TextView icon = convertView.findViewById(R.id.categoryIcon);
            TextView title = convertView.findViewById(R.id.categoryTitle);
            TextView count = convertView.findViewById(R.id.categoryCount);
            TextView arrow = convertView.findViewById(R.id.categoryArrow);

            title.setText(verb.getGermanWord());
            count.setText(verb.getMeaning());
            
            if (isolatedGroupIndex != -1) {
                backArrow.setVisibility(View.VISIBLE);
                arrow.setVisibility(View.GONE);
                icon.setText("🏃");
                backArrow.setOnClickListener(v -> {
                    expandableListView.collapseGroup(0);
                    isolatedGroupIndex = -1;
                    notifyDataSetChanged();
                });
            } else {
                backArrow.setVisibility(View.GONE);
                arrow.setVisibility(View.VISIBLE);
                icon.setText("🏃");
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
                        .inflate(R.layout.item_verb_conjugation, parent, false);
            }

            convertView.findViewById(R.id.verbInfinitive).setVisibility(View.GONE);
            convertView.findViewById(R.id.verbMeaning).setVisibility(View.GONE);

            Conjugator.Conjugation conj = Conjugator.conjugate(word.getGermanWord());
            if (conj != null) {
                setupConjugatedView(convertView.findViewById(R.id.formIch), conj.ich);
                setupConjugatedView(convertView.findViewById(R.id.formDu), conj.du);
                setupConjugatedView(convertView.findViewById(R.id.formErSieEs), conj.erSieEs);
                setupConjugatedView(convertView.findViewById(R.id.formWir), conj.wir);
                setupConjugatedView(convertView.findViewById(R.id.formIhr), conj.ihr);
                setupConjugatedView(convertView.findViewById(R.id.formSieSie), conj.sieSie);
            }

            return convertView;
        }

        private void setupConjugatedView(View view, String text) {
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                tv.setText(text);
                View parent = (View) tv.getParent();
                parent.setOnClickListener(v -> speakGerman(text));
            }
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
