package com.hillal.hhhhhhh.data.room.migrations;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration_3 extends Migration {
    public Migration_3() {
        super(2, 3);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // إضافة الفهرس الجديد إذا لم يكن موجوداً
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_account_currency_date ON transactions(accountId, currency, date)");
    }
} 