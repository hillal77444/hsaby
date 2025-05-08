package com.hillal.hhhhhhh.data.room;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.hillal.hhhhhhh.data.model.PendingOperation;
import java.util.List;

@Dao
public interface PendingOperationDao {
    @Insert
    void insert(PendingOperation operation);

    @Delete
    void delete(PendingOperation operation);

    @Query("SELECT * FROM pending_operations ORDER BY timestamp ASC")
    List<PendingOperation> getAllPendingOperations();

    @Query("DELETE FROM pending_operations WHERE id = :operationId")
    void deleteById(long operationId);
} 