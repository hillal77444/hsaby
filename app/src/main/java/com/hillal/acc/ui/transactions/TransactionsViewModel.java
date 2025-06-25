package com.hillal.acc.ui.transactions;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.hillal.acc.App;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.data.repository.TransactionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

public class TransactionsViewModel extends AndroidViewModel {
    private final TransactionRepository repository;
    private final MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>();
    private long startDate = 0;
    private long endDate = 0;

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

    public void loadTransactionsByDateRange(long startDate, long endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        repository.getTransactionsByDateRange(startDate, endDate).observeForever(transactions::setValue);
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
        loadTransactionsByDateRange(startDate, endDate);
    }

    public void updateTransaction(Transaction transaction) {
        repository.update(transaction);
    }

    public void insertTransaction(Transaction transaction) {
        repository.insert(transaction);
        loadTransactionsByDateRange(startDate, endDate);
    }

    public LiveData<Transaction> getTransactionById(long id) {
        return repository.getTransactionById(id);
    }

    public void filterTransactionsByCurrency(String currency) {
        List<Transaction> currentList = transactions.getValue();
        if (currentList != null) {
            List<Transaction> filteredList = currentList.stream()
                .filter(t -> t.getCurrency() != null && 
                           t.getCurrency().trim().equalsIgnoreCase(currency.trim()))
                .collect(Collectors.toList());
            transactions.setValue(filteredList);
        }
    }

    public void filterTransactionsByAccount(long accountId) {
        List<Transaction> currentList = transactions.getValue();
        if (currentList != null) {
            List<Transaction> filteredList = currentList.stream()
                .filter(t -> t.getAccountId() == accountId)
                .collect(Collectors.toList());
            transactions.setValue(filteredList);
        }
    }

    public LiveData<Map<Long, Map<String, Double>>> getAccountBalancesMap() {
        MutableLiveData<Map<Long, Map<String, Double>>> balancesLiveData = new MutableLiveData<>();
        getTransactions().observeForever(transactionsList -> {
            Map<Long, Map<String, Double>> balancesMap = new HashMap<>();
            if (transactionsList != null) {
                for (Transaction t : transactionsList) {
                    long accountId = t.getAccountId();
                    String currency = t.getCurrency();
                    double amount = t.getAmount();
                    String type = t.getType();
                    if (!balancesMap.containsKey(accountId)) {
                        balancesMap.put(accountId, new HashMap<>());
                    }
                    Map<String, Double> currencyMap = balancesMap.get(accountId);
                    double prev = currencyMap.getOrDefault(currency, 0.0);
                    if (type.equals("عليه") || type.equalsIgnoreCase("debit")) {
                        prev -= amount;
                    } else {
                        prev += amount;
                    }
                    currencyMap.put(currency, prev);
                }
            }
            balancesLiveData.postValue(balancesMap);
        });
        return balancesLiveData;
    }

    public LiveData<List<Transaction>> searchTransactionsByDescription(String query) {
        return repository.searchTransactionsByDescription(query);
    }
} 