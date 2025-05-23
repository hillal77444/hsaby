package com.hillal.hhhhhhh.data.remote;

import com.hillal.hhhhhhh.data.model.User;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/login")
    Call<User> login(@Body LoginRequest request);

    @POST("api/register")
    Call<User> register(@Body RegisterRequest request);

    @POST("api/sync")
    Call<SyncResponse> syncData(@Header("Authorization") String token, @Body SyncRequest request);

    @GET("api/accounts")
    Call<List<Account>> getAccounts(@Header("Authorization") String token);

    @GET("api/transactions")
    Call<List<Transaction>> getTransactions(@Header("Authorization") String token);

    @PUT("api/transactions/{id}")
    Call<Void> updateTransaction(@Header("Authorization") String token, 
                               @Path("id") long transactionId, 
                               @Body Transaction transaction);

    @DELETE("api/transactions/{id}")
    Call<Void> deleteTransaction(@Header("Authorization") String token, 
                               @Path("id") long transactionId);

    @GET("api/sync/changes")
    Call<Map<String, Object>> getChanges(@Header("Authorization") String token, @Query("last_sync") long lastSyncTime);

    @POST("api/sync/changes")
    Call<Map<String, Object>> syncChanges(@Header("Authorization") String token, @Body Map<String, Object> changes);

    @POST("api/refresh-token")
    Call<Map<String, String>> refreshToken(@Header("Authorization") String token);

    @GET("api/server/time")
    Call<Long> getServerTime();

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

    class SyncRequest {
        private List<Account> accounts;
        private List<Transaction> transactions;

        public SyncRequest(List<Account> accounts, List<Transaction> transactions) {
            this.accounts = accounts;
            this.transactions = transactions;
        }

        // Getters for JSON serialization
        public List<Account> getAccounts() {
            return accounts;
        }

        public List<Transaction> getTransactions() {
            // استبعاد last_sync_time من المعاملات
            for (Transaction transaction : transactions) {
                transaction.setLastSyncTime(0);
                // تعيين الحقول الأخرى المطلوبة
                transaction.setCreatedAt(System.currentTimeMillis());
                transaction.setUpdatedAt(System.currentTimeMillis());
            }
            return transactions;
        }
    }

    public static class SyncResponse {
        private Map<Long, Long> accountIdMap;
        private Map<Long, Long> transactionIdMap;

        public Long getAccountServerId(Long localId) {
            return accountIdMap != null ? accountIdMap.get(localId) : null;
        }

        public Long getTransactionServerId(Long localId) {
            return transactionIdMap != null ? transactionIdMap.get(localId) : null;
        }
    }
} 