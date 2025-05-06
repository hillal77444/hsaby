package com.hillal.hhhhhhh.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "transactions",
    foreignKeys = @ForeignKey(
        entity = Account.class,
        parentColumns = "id",
        childColumns = "accountId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("accountId")
)
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long accountId;
    private double amount;
    private boolean isDebit;
    private String notes;
    private long date;

    public Transaction(long accountId, double amount, boolean isDebit, String notes) {
        this.accountId = accountId;
        this.amount = amount;
        this.isDebit = isDebit;
        this.notes = notes;
        this.date = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public boolean isDebit() {
        return isDebit;
    }

    public void setDebit(boolean debit) {
        isDebit = debit;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id == that.id &&
                accountId == that.accountId &&
                Double.compare(that.amount, amount) == 0 &&
                isDebit == that.isDebit &&
                date == that.date &&
                (notes != null ? notes.equals(that.notes) : that.notes == null);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (accountId ^ (accountId >>> 32));
        temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (isDebit ? 1 : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        result = 31 * result + (int) (date ^ (date >>> 32));
        return result;
    }
} 