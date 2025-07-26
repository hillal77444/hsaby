package com.hillal.acc.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.repository.TransactionRepository;
import java.util.Date;
import java.util.List;

public class TransactionViewModel extends AndroidViewModel {
    private final TransactionRepository repository;

    public TransactionViewModel(Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        repository = new TransactionRepository(database);
    }

    public void insert(Transaction transaction) {
        repository.insert(transaction);
    }

    public void update(Transaction transaction) {
        repository.update(transaction);
    }

    public void delete(Transaction transaction) {
        repository.delete(transaction);
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return repository.getAllTransactions();
    }

    public LiveData<List<Transaction>> getTransactionsByAccount(long accountId) {
        return repository.getTransactionsByAccount(accountId);
    }

    public LiveData<List<Transaction>> getTransactionsByDateRange(long fromDate, long toDate) {
        return repository.getTransactionsByDateRange(fromDate, toDate);
    }

    public LiveData<List<Transaction>> getTransactionsByAccountAndDateRange(long accountId, long fromDate, long toDate) {
        return repository.getTransactionsByAccountAndDateRange(accountId, fromDate, toDate);
    }

    public LiveData<Double> getTotalDebit(long accountId) {
        return repository.getTotalDebit(accountId);
    }

    public LiveData<Double> getTotalCredit(long accountId) {
        return repository.getTotalCredit(accountId);
    }

    public LiveData<Double> getTotalCreditForDateRange(long accountId, Date startDate, Date endDate) {
        return repository.getTotalCreditsInDateRange(accountId, startDate.getTime(), endDate.getTime());
    }

    public LiveData<Double> getTotalDebitForDateRange(long accountId, Date startDate, Date endDate) {
        return repository.getTotalDebitsInDateRange(accountId, startDate.getTime(), endDate.getTime());
    }

    public LiveData<Double> getAccountBalance(long accountId) {
        return repository.getAccountBalance(accountId);
    }

    // دوال محسنة للإحصائيات
    public LiveData<Double> getTotalDebtors() {
        return repository.getTotalDebtors();
    }

    public LiveData<Double> getTotalCreditors() {
        return repository.getTotalCreditors();
    }

    public LiveData<Double> getBalanceUntilDate(long accountId, long transactionDate, String currency) {
        return repository.getBalanceUntilDate(accountId, transactionDate, currency);
    }

    public LiveData<Double> getBalanceUntilTransaction(long accountId, long transactionDate, long transactionId, String currency) {
        return repository.getBalanceUntilTransaction(accountId, transactionDate, transactionId, currency);
    }
} 