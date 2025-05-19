package com.hillal.hhhhhhh.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.hillal.hhhhhhh.data.local.entity.PendingOperation;

import java.util.List;

@Dao
public interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations")
    List<PendingOperation> getAllPendingOperations();

    @Query("SELECT * FROM pending_operations WHERE id = :id")
    PendingOperation getPendingOperationById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PendingOperation operation);

    @Update
    void update(PendingOperation operation);

    @Delete
    void delete(PendingOperation operation);

    @Query("DELETE FROM pending_operations")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM pending_operations")
    int getPendingOperationsCount();
} 