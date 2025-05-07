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
    private final MutableLiveData<AuthState> authState = new MutableLiveData<>(AuthState.IDLE);
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
    }

    public void login(String phone, String password) {
        authState.setValue(AuthState.LOADING);
        authRepository.login(phone, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser.postValue(user);
                authState.postValue(AuthState.SUCCESS);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Login error: " + error);
                authState.postValue(AuthState.ERROR);
            }
        });
    }

    public void register(String username, String phone, String password) {
        authState.setValue(AuthState.LOADING);
        authRepository.register(username, phone, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser.postValue(user);
                authState.postValue(AuthState.SUCCESS);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Registration error: " + error);
                authState.postValue(AuthState.ERROR);
            }
        });
    }

    public void logout() {
        authRepository.logout();
        currentUser.setValue(null);
        authState.setValue(AuthState.IDLE);
    }

    public LiveData<AuthState> getAuthState() {
        return authState;
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser.getValue() != null;
    }
} 