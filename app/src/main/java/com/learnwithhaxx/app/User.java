package com.learnwithhaxx.app;

public class User {
    private int id;
    private String name;
    private int streak;
    private String lastActiveDate;

    public User() {}

    public User(String name, int streak, String lastActiveDate) {
        this.name = name;
        this.streak = streak;
        this.lastActiveDate = lastActiveDate;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    public String getLastActiveDate() { return lastActiveDate; }
    public void setLastActiveDate(String lastActiveDate) { this.lastActiveDate = lastActiveDate; }
}
