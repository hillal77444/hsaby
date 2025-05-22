package com.hillal.hhhhhhh.data.room;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.hillal.hhhhhhh.data.model.PendingOperation;
import java.util.List;

@Dao
public interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations")
    LiveData<List<PendingOperation>> getAllPendingOperations();

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
} 