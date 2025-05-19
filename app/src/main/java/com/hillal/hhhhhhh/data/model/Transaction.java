package com.hillal.hhhhhhh.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import androidx.room.ColumnInfo;
import com.google.gson.annotations.SerializedName;
import androidx.room.Index;
import androidx.room.ForeignKey;

import java.util.Date;

@Entity(tableName = "transactions",
        foreignKeys = @ForeignKey(entity = Account.class,
                                parentColumns = "id",
                                childColumns = "account_id",
                                onDelete = ForeignKey.CASCADE),
        indices = {@Index("account_id")})
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "server_id")
    @SerializedName("server_id")
    private long serverId = -1;

    @ColumnInfo(name = "user_id")
    @SerializedName("user_id")
    private long userId;

    @ColumnInfo(name = "account_id")
    @SerializedName("account_id")
    private long accountId;

    @ColumnInfo(name = "amount")
    @SerializedName("amount")
    private double amount;

    @ColumnInfo(name = "type")
    @SerializedName("type")
    private String type;

    @ColumnInfo(name = "description")
    @SerializedName("description")
    private String description;

    @ColumnInfo(name = "notes")
    @SerializedName("notes")
    private String notes;

    @ColumnInfo(name = "currency")
    @SerializedName("currency")
    private String currency;

    @ColumnInfo(name = "transaction_date")
    @SerializedName("date")
    private long transactionDate;

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    private long updatedAt;

    @ColumnInfo(name = "last_sync_time")
    @SerializedName("last_sync_time")
    private long lastSyncTime;

    @ColumnInfo(name = "is_modified")
    @SerializedName("is_modified")
    private boolean isModified;

    @ColumnInfo(name = "whatsapp_enabled")
    @SerializedName("whatsapp_enabled")
    private boolean whatsappEnabled;

    @ColumnInfo(name = "sync_status")
    @SerializedName("sync_status")
    private int syncStatus;

    // Constructor
    @Ignore
    public Transaction(long accountId, double amount, String type, String description, String currency) {
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.currency = currency;
        this.transactionDate = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncStatus = 0; // PENDING
    }

    // Empty constructor for Room
    public Transaction() {
        this.transactionDate = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncStatus = 0; // PENDING
    }

    // Helper method to convert server date to timestamp
    public static long convertServerDateToTimestamp(Object serverDate) {
        if (serverDate == null) {
            return System.currentTimeMillis();
        }
        
        try {
            if (serverDate instanceof Number) {
                // إذا كان التاريخ timestamp
                return ((Number) serverDate).longValue();
            } else if (serverDate instanceof String) {
                // إذا كان التاريخ نص ISO
                String dateStr = (String) serverDate;
                return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .parse(dateStr)
                    .getTime();
            }
        } catch (Exception e) {
            // في حالة حدوث أي خطأ، نستخدم الوقت الحالي
            return System.currentTimeMillis();
        }
        return System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(long transactionDate) {
        this.transactionDate = transactionDate;
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

    public long getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setModified(boolean modified) {
        isModified = modified;
    }

    public boolean isWhatsappEnabled() {
        return whatsappEnabled;
    }

    public void setWhatsappEnabled(boolean whatsappEnabled) {
        this.whatsappEnabled = whatsappEnabled;
    }

    public int getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(int syncStatus) {
        this.syncStatus = syncStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id == that.id &&
               accountId == that.accountId &&
               Double.compare(that.amount, amount) == 0 &&
               transactionDate == that.transactionDate &&
               createdAt == that.createdAt &&
               updatedAt == that.updatedAt &&
               type.equals(that.type) &&
               description.equals(that.description) &&
               currency.equals(that.currency) &&
               (notes == null ? that.notes == null : notes.equals(that.notes));
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (accountId ^ (accountId >>> 32));
        temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + type.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        result = 31 * result + currency.hashCode();
        result = 31 * result + (int) (transactionDate ^ (transactionDate >>> 32));
        result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
        result = 31 * result + (int) (updatedAt ^ (updatedAt >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", accountId=" + accountId +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", whatsappEnabled=" + whatsappEnabled +
                ", transactionDate=" + transactionDate +
                ", lastSyncTime=" + lastSyncTime +
                ", isModified=" + isModified +
                '}';
    }
} 