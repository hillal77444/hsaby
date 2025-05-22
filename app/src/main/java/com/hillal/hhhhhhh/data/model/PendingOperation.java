package com.hillal.hhhhhhh.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_operations")
public class PendingOperation {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String operationType; // "UPDATE" or "DELETE"
    private long transactionId;
    private String transactionData; // JSON string of transaction data for updates
    private long timestamp;

    public PendingOperation(String operationType, long transactionId, String transactionData) {
        this.operationType = operationType;
        this.transactionId = transactionId;
        this.transactionData = transactionData;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    
    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }
    
    public String getTransactionData() { return transactionData; }
    public void setTransactionData(String transactionData) { this.transactionData = transactionData; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 