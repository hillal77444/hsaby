package com.hillal.acc.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.lifecycle.LiveData;

import com.hillal.acc.data.entities.Cashbox;

import java.util.List;

@Dao
public interface CashboxDao {
    @Query("SELECT * FROM cashboxes")
    LiveData<List<Cashbox>> getAll();

    @Query("SELECT * FROM cashboxes")
    List<Cashbox> getAllSync();

    @Insert
    void insert(Cashbox cashbox);

    @Update
    void update(Cashbox cashbox);

    @Delete
    void delete(Cashbox cashbox);

    @Query("DELETE FROM cashboxes")
    void deleteAll();
} 