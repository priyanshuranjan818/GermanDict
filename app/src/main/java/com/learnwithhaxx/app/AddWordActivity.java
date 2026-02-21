package com.learnwithhaxx.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

public class AddWordActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;
    private static final int CREATE_FILE_REQUEST = 2;

    private DatabaseHelper db;
    private EditText inputGermanWord, inputMeaning, inputExample;
    private Spinner spinnerPartOfSpeech;
    private TextView errorMessage;
    private RecyclerView manageRecyclerView;
    private TextView emptyManage;
    private ImageButton exportImportBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_word);

        db = DatabaseHelper.getInstance(this);

        // Views
        inputGermanWord = findViewById(R.id.inputGermanWord);
        inputMeaning = findViewById(R.id.inputMeaning);
        inputExample = findViewById(R.id.inputExample);
        spinnerPartOfSpeech = findViewById(R.id.spinnerPartOfSpeech);
        errorMessage = findViewById(R.id.errorMessage);
        manageRecyclerView = findViewById(R.id.manageRecyclerView);
        emptyManage = findViewById(R.id.emptyManage);
        exportImportBtn = findViewById(R.id.exportImportBtn);

        manageRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Back button
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // Export/Import button
        exportImportBtn.setOnClickListener(v -> showExportImportDialog());

        // Submit button
        Button submitBtn = findViewById(R.id.submitBtn);
        submitBtn.setOnClickListener(v -> saveWord());

        // Bottom Navigation
        setupBottomNav();

        // Load existing words
        loadManageList();
    }

    private void showExportImportDialog() {
        String[] options = {"Export Words (CSV)", "Import Words (CSV)"};
        new AlertDialog.Builder(this)
                .setTitle("Backup & Restore")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportWords();
                    } else {
                        importWords();
                    }
                })
                .show();
    }

    private void exportWords() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/comma-separated-values");
        intent.putExtra(Intent.EXTRA_TITLE, "german_words_backup.csv");
        startActivityForResult(intent, CREATE_FILE_REQUEST);
    }

    private void importWords() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == CREATE_FILE_REQUEST) {
                processExport(uri);
            } else if (requestCode == PICK_FILE_REQUEST) {
                processImport(uri);
            }
        }
    }

    private void processExport(Uri uri) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream == null) return;

            StringBuilder csv = new StringBuilder();
            csv.append("German,Meaning,Example,Category\n");

            List<Word> words = db.getAllWords();
            for (Word w : words) {
                csv.append(escapeCsv(w.getGermanWord())).append(",")
                   .append(escapeCsv(w.getMeaning())).append(",")
                   .append(escapeCsv(w.getExample())).append(",")
                   .append(escapeCsv(w.getPartOfSpeech())).append("\n");
            }

            outputStream.write(csv.toString().getBytes());
            outputStream.close();
            Toast.makeText(this, "Words exported successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processImport(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int count = 0;
            boolean firstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    if (line.toLowerCase().contains("german")) continue; 
                }
                
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length >= 2) {
                    String german = unescapeCsv(parts[0]);
                    String meaning = unescapeCsv(parts[1]);
                    String example = parts.length > 2 ? unescapeCsv(parts[2]) : "";
                    String category = parts.length > 3 ? unescapeCsv(parts[3]) : "";

                    if (!db.isDuplicateWord(german)) {
                        db.addWord(german, meaning, example, category);
                        count++;
                    }
                }
            }
            reader.close();
            loadManageList();
            Toast.makeText(this, "Imported " + count + " new words!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private String unescapeCsv(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            str = str.substring(1, str.length() - 1);
            return str.replace("\"\"", "\"");
        }
        return str;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadManageList();
    }

    private void saveWord() {
        String germanWord = inputGermanWord.getText().toString().trim();
        String meaning = inputMeaning.getText().toString().trim();
        String example = inputExample.getText().toString().trim();
        int spinnerPos = spinnerPartOfSpeech.getSelectedItemPosition();
        String partOfSpeech = spinnerPos > 0 ? spinnerPartOfSpeech.getSelectedItem().toString() : "";

        // Validate
        if (germanWord.isEmpty() || meaning.isEmpty()) {
            Toast.makeText(this, "Please fill in the German word and meaning", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spinnerPos == 0) {
            Toast.makeText(this, "Please choose a category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check duplicate
        if (db.isDuplicateWord(germanWord)) {
            errorMessage.setText(String.format(getString(R.string.duplicate_error), germanWord));
            errorMessage.setVisibility(View.VISIBLE);
            return;
        }

        // Save
        db.addWord(germanWord, meaning, example, partOfSpeech);
        db.updateStreak();

        // Clear form
        inputGermanWord.setText("");
        inputMeaning.setText("");
        inputExample.setText("");
        spinnerPartOfSpeech.setSelection(0);
        errorMessage.setVisibility(View.GONE);

        Toast.makeText(this, "Word added!", Toast.LENGTH_SHORT).show();

        // Reload manage list
        loadManageList();

        // Focus back to first input
        inputGermanWord.requestFocus();
    }

    private void loadManageList() {
        List<Word> words = db.getAllWords();
        if (words.isEmpty()) {
            manageRecyclerView.setVisibility(View.GONE);
            emptyManage.setVisibility(View.VISIBLE);
        } else {
            manageRecyclerView.setVisibility(View.VISIBLE);
            emptyManage.setVisibility(View.GONE);
            manageRecyclerView.setAdapter(new ManageAdapter(words, word -> {
                new AlertDialog.Builder(AddWordActivity.this)
                        .setTitle("Delete Word")
                        .setMessage("Delete \"" + word.getGermanWord() + "\"?")
                        .setPositiveButton("Delete", (d, w) -> {
                            db.deleteWord(word.getId());
                            loadManageList();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }));
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_add);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
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

    // ─── Manage Adapter ──────────────────────────────────

    public interface OnDeleteClickListener {
        void onDelete(Word word);
    }

    private static class ManageAdapter extends RecyclerView.Adapter<ManageAdapter.VH> {
        private final List<Word> words;
        private final OnDeleteClickListener deleteListener;

        ManageAdapter(List<Word> words, OnDeleteClickListener deleteListener) {
            this.words = words;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_word_manage, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Word word = words.get(position);
            holder.germanWord.setText(word.getGermanWord());
            holder.meaning.setText(word.getMeaning());

            if (word.getPartOfSpeech() != null && !word.getPartOfSpeech().isEmpty()) {
                holder.category.setText(word.getPartOfSpeech());
                holder.category.setVisibility(View.VISIBLE);
            } else {
                holder.category.setVisibility(View.GONE);
            }

            holder.deleteBtn.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(word);
                }
            });
        }

        @Override
        public int getItemCount() {
            return words.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView germanWord, meaning, category;
            View deleteBtn;
            VH(View v) {
                super(v);
                germanWord = v.findViewById(R.id.germanWord);
                meaning = v.findViewById(R.id.wordMeaning);
                category = v.findViewById(R.id.wordCategory);
                deleteBtn = v.findViewById(R.id.deleteBtn);
            }
        }
    }
}
