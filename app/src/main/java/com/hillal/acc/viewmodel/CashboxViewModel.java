package com.hillal.acc.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.data.repository.CashboxRepository;

import java.util.List;

public class CashboxViewModel extends AndroidViewModel {
    private CashboxRepository repository;

    public CashboxViewModel(@NonNull Application application) {
        super(application);
        repository = new CashboxRepository(application);
    }

    public LiveData<List<Cashbox>> getAllCashboxes() {
        return repository.getAllCashboxes();
    }

    public void insert(Cashbox cashbox) {
        repository.insert(cashbox);
    }

    public void update(Cashbox cashbox) {
        repository.update(cashbox);
    }

    public void delete(Cashbox cashbox) {
        repository.delete(cashbox);
    }

    public void deleteAll() {
        repository.deleteAll();
    }

    public void fetchCashboxesFromApi(String token, Runnable onDone) {
        repository.fetchCashboxesFromApi(token, onDone);
    }

    public void addCashboxToServer(String token, String name, CashboxRepository.CashboxCallback callback) {
        repository.addCashboxToServer(token, name, callback);
    }
} 