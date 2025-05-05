package com.hillal.hhhhhhh.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.hillal.hhhhhhh.data.entities.Report;
import java.util.List;

@Dao
public interface ReportDao {
    @Insert
    void insert(Report report);

    @Update
    void update(Report report);

    @Delete
    void delete(Report report);

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    LiveData<List<Report>> getAllReports();

    @Query("SELECT * FROM reports WHERE id = :id")
    LiveData<Report> getReportById(long id);
} 