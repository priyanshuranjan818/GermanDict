package com.learnwithhaxx.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AddWordActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;
    private static final int CREATE_FILE_REQUEST = 2;

    private DatabaseHelper db;
    private EditText inputGermanWord, inputMeaning, inputExample;
    private TextView errorMessage;
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
        errorMessage = findViewById(R.id.errorMessage);
        exportImportBtn = findViewById(R.id.exportImportBtn);

        // Back button
        findViewById(R.id.backBtn).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        // Export/Import button
        exportImportBtn.setOnClickListener(v -> showExportImportDialog());

        // Submit button
        Button submitBtn = findViewById(R.id.submitBtn);
        submitBtn.setOnClickListener(v -> saveWord());

        // Focus on German word input
        inputGermanWord.requestFocus();

        // Bottom Navigation
        setupBottomNav();
    }

    private void showExportImportDialog() {
        String[] options = {"Export Words (CSV)", "Import Words (CSV)"};
        new AlertDialog.Builder(this)
                .setTitle("Backup & Restore")
                .setItems(options, (dialog, prefix) -> {
                    if (prefix == 0) {
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
            List<String> lines = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();

            if (lines.isEmpty()) return;

            int startIndex = 0;
            // Check if first line is header
            if (lines.get(0).toLowerCase().contains("german")) {
                startIndex = 1;
            }

            int count = 0;
            // Iterate backwards so the top word in CSV is inserted last and appears at the top (since list is ordered by id DESC)
            for (int i = lines.size() - 1; i >= startIndex; i--) {
                String l = lines.get(i);
                String[] parts = l.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
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

    private void saveWord() {
        String germanWord = inputGermanWord.getText().toString().trim();
        String meaning = inputMeaning.getText().toString().trim();
        String example = inputExample.getText().toString().trim();
        
        // Auto-assign category Nomen if it starts with der/die/das
        String lowerWord = germanWord.toLowerCase();
        String partOfSpeech = "General";
        if (lowerWord.startsWith("der ") || lowerWord.startsWith("die ") || lowerWord.startsWith("das ")) {
            partOfSpeech = "Nomen";
        }

        // Validate
        if (germanWord.isEmpty() || meaning.isEmpty()) {
            Toast.makeText(this, "Please fill in the German word and meaning", Toast.LENGTH_SHORT).show();
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
        errorMessage.setVisibility(View.GONE);

        Toast.makeText(this, "Word added!", Toast.LENGTH_SHORT).show();

        // Focus back to first input
        inputGermanWord.requestFocus();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_add);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            } else if (id == R.id.nav_add) {
                return true;
            } else if (id == R.id.nav_nouns) {
                startActivity(new Intent(this, NounsActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
}
