package com.hillal.hhhhhhh.data.room.migrations;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration_5 extends Migration {
    public Migration_5() {
        super(4, 5);
    }

    @Override
    public void migrate(SupportSQLiteDatabase database) {
        // إنشاء جدول مؤقت بالهيكل الجديد
        database.execSQL("CREATE TABLE IF NOT EXISTS transactions_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "server_id INTEGER NOT NULL DEFAULT 0, " +
                "user_id INTEGER NOT NULL DEFAULT 0, " +
                "account_id INTEGER NOT NULL, " +
                "amount REAL NOT NULL, " +
                "type TEXT NOT NULL, " +
                "description TEXT, " +
                "notes TEXT, " +
                "currency TEXT, " +
                "transaction_date INTEGER NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, " +
                "last_sync_time INTEGER NOT NULL DEFAULT 0, " +
                "is_modified INTEGER NOT NULL DEFAULT 0, " +
                "whatsapp_enabled INTEGER NOT NULL DEFAULT 1, " +
                "sync_status INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE)");

        // نسخ البيانات من الجدول القديم إلى الجدول الجديد
        database.execSQL("INSERT INTO transactions_new (id, account_id, amount, description, transaction_date, type) " +
                "SELECT id, accountId, amount, description, date, CASE WHEN isCredit = 1 THEN 'credit' ELSE 'debit' END " +
                "FROM transactions");

        // حذف الجدول القديم
        database.execSQL("DROP TABLE transactions");

        // إعادة تسمية الجدول الجديد
        database.execSQL("ALTER TABLE transactions_new RENAME TO transactions");

        // إنشاء الفهرس
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_account_id ON transactions (account_id)");
    }
} 