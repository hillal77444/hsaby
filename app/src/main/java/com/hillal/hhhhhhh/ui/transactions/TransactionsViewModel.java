package com.hillal.hhhhhhh.ui.transactions;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.hillal.hhhhhhh.App;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.repository.TransactionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class TransactionsViewModel extends AndroidViewModel {
    private final TransactionRepository repository;
    private final MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>();

    public TransactionsViewModel(Application application) {
        super(application);
        repository = ((App) application).getTransactionRepository();
        loadAllTransactions();
    }

    public LiveData<List<Transaction>> getTransactions() {
        return transactions;
    }

    public void loadAllTransactions() {
        repository.getAllTransactions().observeForever(transactions::setValue);
    }

    public void loadTransactionsByAccount(String accountName) {
        repository.getAllTransactions().observeForever(transactionList -> {
            if (transactionList != null) {
                List<Transaction> filteredTransactions = new ArrayList<>();
                for (Transaction t : transactionList) {
                    if (accountName.equals(String.valueOf(t.getAccountId()))) {
                        filteredTransactions.add(t);
                    }
                }
                transactions.setValue(filteredTransactions);
            }
        });
    }

    public void loadTransactionsByDateRange(Date startDate, Date endDate) {
        repository.getAllTransactions().observeForever(transactionList -> {
            if (transactionList != null) {
                List<Transaction> filteredTransactions = new ArrayList<>();
                for (Transaction t : transactionList) {
                    Date transactionDate = new Date(t.getDate());
                    if (transactionDate != null && 
                        !transactionDate.before(startDate) && 
                        !transactionDate.after(endDate)) {
                        filteredTransactions.add(t);
                    }
                }
                transactions.setValue(filteredTransactions);
            }
        });
    }

    public void loadTransactionsByCurrency(String currency) {
        repository.getAllTransactions().observeForever(transactionList -> {
            if (transactionList != null) {
                List<Transaction> filteredTransactions = new ArrayList<>();
                for (Transaction t : transactionList) {
                    if (currency.equals(t.getCurrency())) {
                        filteredTransactions.add(t);
                    }
                }
                transactions.setValue(filteredTransactions);
            }
        });
    }

    public void deleteTransaction(Transaction transaction) {
        repository.delete(transaction);
    }

    public void updateTransaction(Transaction transaction) {
        repository.update(transaction);
    }

    public void insertTransaction(Transaction transaction) {
        repository.insert(transaction);
    }

    public LiveData<Transaction> getTransactionById(long id) {
        return repository.getTransactionById(id);
    }
} 