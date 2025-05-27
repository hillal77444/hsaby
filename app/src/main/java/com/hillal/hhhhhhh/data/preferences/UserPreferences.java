package com.hillal.hhhhhhh.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferences {
    private static final String PREF_NAME = "user_preferences";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_PHONE = "user_phone";
    
    private final SharedPreferences preferences;

    public UserPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserName(String name) {
        preferences.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, "");
    }

    public void savePhoneNumber(String phone) {
        preferences.edit().putString(KEY_PHONE, phone).apply();
    }

    public String getPhoneNumber() {
        return preferences.getString(KEY_PHONE, "");
    }
} 