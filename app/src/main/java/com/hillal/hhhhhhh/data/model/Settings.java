package com.hillal.hhhhhhh.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "settings")
public class Settings {
    @PrimaryKey
    private int id = 1; // We'll only have one settings record
    
    private String defaultCurrency;
    private String theme;
    private boolean notificationsEnabled;
    private String language;
    private long createdAt;
    private long updatedAt;

    public Settings() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.defaultCurrency = "USD";
        this.theme = "light";
        this.notificationsEnabled = true;
        this.language = "en";
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
} 