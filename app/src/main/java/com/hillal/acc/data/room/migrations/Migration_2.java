package com.hillal.acc.data.room.migrations;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration_2 extends Migration {
    public Migration_2() {
        super(1, 2);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // إضافة عمود whatsapp_enabled إلى جدول transactions
        database.execSQL("ALTER TABLE transactions ADD COLUMN whatsapp_enabled INTEGER NOT NULL DEFAULT 1");
        // إضافة الفهرس الجديد لتسريع الاستعلامات
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_account_currency_date ON transactions(accountId, currency, date)");
    }
} 