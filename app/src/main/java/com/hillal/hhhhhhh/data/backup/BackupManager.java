package com.hillal.hhhhhhh.data.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.hillal.hhhhhhh.data.model.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupManager {
    private static final String TAG = "BackupManager";
    private static final String BACKUP_DIR = "backups";
    private final Context context;
    private final Gson gson;

    public BackupManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public interface BackupCallback {
        void onSuccess(String backupPath);
        void onError(String error);
    }

    public void createBackup(BackupCallback callback) {
        try {
            // إنشاء مجلد النسخ الاحتياطي إذا لم يكن موجوداً
            File backupDir = new File(context.getFilesDir(), BACKUP_DIR);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            // إنشاء اسم الملف مع التاريخ والوقت
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String backupFileName = "backup_" + timestamp + ".json";
            File backupFile = new File(backupDir, backupFileName);

            // جمع البيانات للنسخ الاحتياطي
            BackupData backupData = new BackupData();
            backupData.setTimestamp(timestamp);
            backupData.setUserData(getUserData());

            // تحويل البيانات إلى JSON وحفظها
            String jsonData = gson.toJson(backupData);
            try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                fos.write(jsonData.getBytes());
                callback.onSuccess(backupFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error creating backup: " + e.getMessage());
            callback.onError("فشل إنشاء النسخة الاحتياطية");
        }
    }

    public void restoreBackup(String backupPath, BackupCallback callback) {
        try {
            File backupFile = new File(backupPath);
            if (!backupFile.exists()) {
                callback.onError("ملف النسخة الاحتياطية غير موجود");
                return;
            }

            // قراءة البيانات من الملف
            String jsonData = new String(java.nio.file.Files.readAllBytes(backupFile.toPath()));
            BackupData backupData = gson.fromJson(jsonData, BackupData.class);

            // استعادة البيانات
            restoreUserData(backupData.getUserData());
            callback.onSuccess("تم استعادة النسخة الاحتياطية بنجاح");
        } catch (Exception e) {
            Log.e(TAG, "Error restoring backup: " + e.getMessage());
            callback.onError("فشل استعادة النسخة الاحتياطية");
        }
    }

    private User getUserData() {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        return new User(
            prefs.getLong("user_id", -1),
            prefs.getString("username", ""),
            prefs.getString("phone", ""),
            prefs.getString("token", "")
        );
    }

    private void restoreUserData(User user) {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        prefs.edit()
            .putLong("user_id", user.getId())
            .putString("username", user.getUsername())
            .putString("phone", user.getPhone())
            .putString("token", user.getToken())
            .apply();
    }

    private static class BackupData {
        private String timestamp;
        private User userData;

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public User getUserData() {
            return userData;
        }

        public void setUserData(User userData) {
            this.userData = userData;
        }
    }
} 