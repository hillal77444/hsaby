package com.hillal.hhhhhhh.network;

import com.hillal.hhhhhhh.models.AccountSummaryResponse;
import com.hillal.hhhhhhh.models.User;
import com.hillal.hhhhhhh.models.Account;
import com.hillal.hhhhhhh.models.Transaction;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface ApiService {
    @GET("api/accounts/summary/{phone}")
    Call<AccountSummaryResponse> getAccountSummary(@Path("phone") String phone);

    @POST("api/login")
    Call<User> login(@Body LoginRequest request);

    @POST("api/register")
    Call<User> register(@Body RegisterRequest request);

    @GET("api/accounts")
    Call<List<Account>> getAccounts(@Header("Authorization") String token);

    @GET("api/transactions")
    Call<List<Transaction>> getTransactions(@Header("Authorization") String token);

    class LoginRequest {
        private String phone;
        private String password;

        public LoginRequest(String phone, String password) {
            this.phone = phone;
            this.password = password;
        }
    }

    class RegisterRequest {
        private String username;
        private String phone;
        private String password;

        public RegisterRequest(String username, String phone, String password) {
            this.username = username;
            this.phone = phone;
            this.password = password;
        }
    }
} 