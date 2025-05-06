package com.hillal.hhhhhhh.data.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.hillal.hhhhhhh.data.model.Settings;
import java.util.List;

@Dao
public interface SettingsDao {
    @Insert
    void insert(Settings settings);

    @Update
    void update(Settings settings);

    @Delete
    void delete(Settings settings);

    @Query("SELECT * FROM settings")
    LiveData<List<Settings>> getAllSettings();

    @Query("SELECT * FROM settings WHERE settings.key = :key")
    LiveData<Settings> getSettingByKey(String key);
} 