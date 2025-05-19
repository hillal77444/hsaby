package com.hillal.hhhhhhh.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_operations")
public class PendingOperation {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String operationType; // INSERT, UPDATE, DELETE
    private String entityType; // ACCOUNT, TRANSACTION
    private String entityData; // JSON string of the entity
    private long timestamp;
    private int retryCount;
    private int status; // 0: pending, 1: in progress, 2: completed, 3: failed

    public PendingOperation() {
        this.timestamp = System.currentTimeMillis();
        this.retryCount = 0;
        this.status = 0;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityData() {
        return entityData;
    }

    public void setEntityData(String entityData) {
        this.entityData = entityData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
} 