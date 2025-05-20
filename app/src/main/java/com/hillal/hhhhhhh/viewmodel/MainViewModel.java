package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.sync.SyncManager;
import com.hillal.hhhhhhh.data.room.AppDatabase;
import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final TransactionDao transactionDao;
    private final AccountDao accountDao;
    private final SyncManager syncManager;
    private final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);
    private final MutableLiveData<String> syncError = new MutableLiveData<>();

    // ثوابت حالة المزامنة
    public static final int SYNC_STATUS_PENDING = -1;  // في الانتظار
    public static final int SYNC_STATUS_SYNCED = 0;    // مزامن

    public MainViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        transactionDao = db.transactionDao();
        accountDao = db.accountDao();
        syncManager = new SyncManager(application, accountDao, transactionDao);
    }

    // الحصول على جميع المعاملات
    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    // الحصول على المعاملات المزامنة
    public LiveData<List<Transaction>> getSyncedTransactions() {
        return transactionDao.getTransactionsBySyncStatus(SYNC_STATUS_SYNCED);
    }

    // الحصول على المعاملات في انتظار المزامنة
    public LiveData<List<Transaction>> getPendingTransactions() {
        return transactionDao.getTransactionsBySyncStatus(SYNC_STATUS_PENDING);
    }

    // الحصول على حالة المزامنة
    public LiveData<Boolean> getIsSyncing() {
        return isSyncing;
    }

    // الحصول على أخطاء المزامنة
    public LiveData<String> getSyncError() {
        return syncError;
    }

    // بدء المزامنة
    public void startSync() {
        if (isSyncing.getValue() != null && isSyncing.getValue()) {
            return; // تجنب المزامنة المتزامنة
        }

        isSyncing.setValue(true);
        syncError.setValue(null);

        syncManager.syncChanges(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                isSyncing.postValue(false);
            }

            @Override
            public void onError(String error) {
                syncError.postValue(error);
                isSyncing.postValue(false);
            }
        });
    }

    // إيقاف المزامنة
    public void stopSync() {
        syncManager.stopPeriodicSync();
    }
} 