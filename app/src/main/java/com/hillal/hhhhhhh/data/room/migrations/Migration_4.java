package com.hillal.hhhhhhh.data.room.migrations;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration_4 extends Migration {
    public Migration_4() {
        super(3, 4);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        try {
            // تحديث أسماء العملات في جدول المعاملات
            database.beginTransaction();
            
            // تحديث العملة اليمنية
            database.execSQL("UPDATE transactions SET currency = 'يمني' WHERE currency = 'ريال يمني'");
            
            // تحديث العملة السعودية
            database.execSQL("UPDATE transactions SET currency = 'سعودي' WHERE currency = 'ريال سعودي'");
            
            // تحديث العملة الأمريكية
            database.execSQL("UPDATE transactions SET currency = 'دولار' WHERE currency = 'دولار أمريكي'");
            
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }
} 