package com.hillal.hhhhhhh.data.room;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.hillal.hhhhhhh.data.model.PendingOperation;
import java.util.List;

@Dao
public interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations")
    LiveData<List<PendingOperation>> getAllPendingOperations();

    @Query("SELECT * FROM pending_operations WHERE status = :status")
    LiveData<List<PendingOperation>> getPendingOperationsByStatus(int status);

    @Query("SELECT COUNT(*) FROM pending_operations")
    int getPendingOperationsCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PendingOperation operation);

    @Update
    void update(PendingOperation operation);

    @Delete
    void delete(PendingOperation operation);

    @Query("DELETE FROM pending_operations")
    void deleteAllPendingOperations();

    @Query("SELECT * FROM pending_operations WHERE entityType = :entityType AND status = 0")
    LiveData<List<PendingOperation>> getPendingOperationsByEntityType(String entityType);
} 