package com.hillal.hhhhhhh.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.hillal.hhhhhhh.data.Converters;
import java.util.Date;

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
@TypeConverters({Converters.class})
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long accountId;
    private double amount;
    private String currency; // ريال يمني، ريال سعودي، دولار
    private String description;
    private String type; // مدين، دائن
    private Date date;
    private String referenceNumber; // رقم المرجع
    private String notes;

    public Transaction() {
        this.date = new Date();
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id == that.id &&
                accountId == that.accountId &&
                Double.compare(that.amount, amount) == 0 &&
                (currency != null ? currency.equals(that.currency) : that.currency == null) &&
                (description != null ? description.equals(that.description) : that.description == null) &&
                (type != null ? type.equals(that.type) : that.type == null) &&
                (date != null ? date.equals(that.date) : that.date == null) &&
                (referenceNumber != null ? referenceNumber.equals(that.referenceNumber) : that.referenceNumber == null) &&
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
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (referenceNumber != null ? referenceNumber.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        return result;
    }
} 