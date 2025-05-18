package com.hillal.hhhhhhh.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.room.ColumnInfo;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "accounts",
        indices = {@Index(value = {"phone_number"}, unique = true)})
public class Account {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "server_id")
    @SerializedName("server_id")
    private long serverId;

    @ColumnInfo(name = "user_id")
    @SerializedName("user_id")
    private long userId;

    @ColumnInfo(name = "account_number")
    @SerializedName("account_number")
    private String accountNumber;

    @ColumnInfo(name = "account_name")
    @SerializedName("account_name")
    private String name;

    @ColumnInfo(name = "balance")
    @SerializedName("balance")
    private double balance;

    @ColumnInfo(name = "phone_number")
    @SerializedName("phone_number")
    private String phoneNumber;

    @ColumnInfo(name = "notes")
    @SerializedName("notes")
    private String notes;

    @ColumnInfo(name = "is_debtor")
    @SerializedName("is_debtor")
    private boolean isDebtor;

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

    @ColumnInfo(name = "currency")
    @SerializedName("currency")
    private String currency;

    // Constructor
    @Ignore
    public Account(String accountNumber, String name, double balance, String phoneNumber, boolean isDebtor) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.balance = balance;
        this.phoneNumber = phoneNumber;
        this.isDebtor = isDebtor;
        this.whatsappEnabled = true; // تفعيل واتساب افتراضياً
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.syncStatus = 0; // PENDING
    }

    // Empty constructor for Room
    public Account() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.whatsappEnabled = true; // تفعيل واتساب افتراضياً
        this.syncStatus = 0; // PENDING
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isDebtor() {
        return isDebtor;
    }

    public void setIsDebtor(boolean isDebtor) {
        this.isDebtor = isDebtor;
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
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", balance=" + balance +
                ", isDebtor=" + isDebtor +
                ", whatsappEnabled=" + whatsappEnabled +
                ", lastSyncTime=" + lastSyncTime +
                ", isModified=" + isModified +
                '}';
    }
} 