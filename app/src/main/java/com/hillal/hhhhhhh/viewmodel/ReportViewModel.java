package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.hillal.hhhhhhh.data.AppDatabase;
import com.hillal.hhhhhhh.data.dao.ReportDao;
import com.hillal.hhhhhhh.data.entities.Report;
import java.util.List;

public class ReportViewModel extends AndroidViewModel {
    private final ReportDao reportDao;
    private final LiveData<List<Report>> allReports;

    public ReportViewModel(Application application) {
        super(application);
        reportDao = AppDatabase.getInstance(application).reportDao();
        allReports = reportDao.getAllReports();
    }

    public LiveData<List<Report>> getAllReports() {
        return allReports;
    }

    public void insert(Report report) {
        AppDatabase.databaseWriteExecutor.execute(() -> reportDao.insert(report));
    }

    public void update(Report report) {
        AppDatabase.databaseWriteExecutor.execute(() -> reportDao.update(report));
    }

    public void delete(Report report) {
        AppDatabase.databaseWriteExecutor.execute(() -> reportDao.delete(report));
    }
} 