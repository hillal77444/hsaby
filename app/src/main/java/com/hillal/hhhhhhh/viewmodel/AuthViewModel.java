package com.hillal.hhhhhhh.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hillal.hhhhhhh.data.repository.AuthRepository;
import com.hillal.hhhhhhh.data.model.AuthState;
import com.hillal.hhhhhhh.data.model.User;

public class AuthViewModel extends AndroidViewModel {
    private static final String TAG = "AuthViewModel";
    private final AuthRepository authRepository;
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>();

    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }

    public enum AuthState {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
    }

    public void login(String phone, String password, AuthCallback callback) {
        authState.setValue(AuthState.LOADING);
        authRepository.login(phone, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                authState.postValue(AuthState.SUCCESS);
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                authState.postValue(AuthState.ERROR);
                callback.onError(error);
            }
        });
    }

    public void register(String username, String phone, String password, AuthCallback callback) {
        authState.setValue(AuthState.LOADING);
        authRepository.register(username, phone, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                authState.postValue(AuthState.SUCCESS);
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                authState.postValue(AuthState.ERROR);
                callback.onError(error);
            }
        });
    }

    public void logout() {
        authRepository.logout();
        authState.setValue(AuthState.IDLE);
    }

    public LiveData<AuthState> getAuthState() {
        return authState;
    }

    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    public User getCurrentUser() {
        return authRepository.getCurrentUser();
    }
} 