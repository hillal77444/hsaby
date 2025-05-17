package com.hillal.hhhhhhh.data.room.migrations;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration_4 extends Migration {
    public Migration_4() {
        super(3, 4);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // نسخ جدول المعاملات القديم
        database.execSQL("CREATE TABLE IF NOT EXISTS transactions_backup AS SELECT * FROM transactions");
        
        // تحديث أسماء العملات في الجدول القديم
        database.execSQL("UPDATE transactions SET currency = 'يمني' WHERE currency = 'ريال يمني'");
        database.execSQL("UPDATE transactions SET currency = 'سعودي' WHERE currency = 'ريال سعودي'");
        database.execSQL("UPDATE transactions SET currency = 'دولار' WHERE currency = 'دولار أمريكي'");
        
        // حذف الجدول المؤقت
        database.execSQL("DROP TABLE IF EXISTS transactions_backup");
    }
} 