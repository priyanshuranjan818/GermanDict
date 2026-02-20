package com.learnwithhaxx.app;

public class Word {
    private int id;
    private int userId;
    private String germanWord;
    private String meaning;
    private String example;
    private String partOfSpeech;
    private String dateAdded;

    public Word() {}

    public Word(int userId, String germanWord, String meaning, String example, String partOfSpeech, String dateAdded) {
        this.userId = userId;
        this.germanWord = germanWord;
        this.meaning = meaning;
        this.example = example;
        this.partOfSpeech = partOfSpeech;
        this.dateAdded = dateAdded;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getGermanWord() { return germanWord; }
    public void setGermanWord(String germanWord) { this.germanWord = germanWord; }

    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public String getPartOfSpeech() { return partOfSpeech; }
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }

    public String getDateAdded() { return dateAdded; }
    public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }
}
