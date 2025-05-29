package com.hillal.acc.data.remote;

import com.hillal.acc.data.model.User;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import retrofit2.http.Header;
import retrofit2.http.Query;
import com.google.gson.annotations.SerializedName;

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

    @POST("/api/update_username")
    Call<User> updateUserName(
        @Header("Authorization") String token,
        @Body Map<String, String> body
    );

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

    public static class SyncRequest {
        private List<Account> accounts;
        private List<Transaction> new_transactions;
        private List<Transaction> modified_transactions;

        public SyncRequest(List<Account> accounts, List<Transaction> newTransactions, List<Transaction> modifiedTransactions) {
            this.accounts = accounts;
            this.new_transactions = newTransactions;
            this.modified_transactions = modifiedTransactions;
        }

        public List<Account> getAccounts() {
            return accounts;
        }

        public List<Transaction> getNewTransactions() {
            return new_transactions;
        }

        public List<Transaction> getModifiedTransactions() {
            return modified_transactions;
        }
    }

    public static class SyncResponse {
        @SerializedName("account_mappings")
        private List<Mapping> accountMappings;
        
        @SerializedName("transaction_mappings")
        private List<Mapping> transactionMappings;

        public Long getAccountServerId(Long localId) {
            if (accountMappings != null) {
                for (Mapping mapping : accountMappings) {
                    if (mapping.localId.equals(localId)) {
                        return mapping.serverId;
                    }
                }
            }
            return null;
        }

        public Long getTransactionServerId(Long localId) {
            if (transactionMappings != null) {
                for (Mapping mapping : transactionMappings) {
                    if (mapping.localId.equals(localId)) {
                        return mapping.serverId;
                    }
                }
            }
            return null;
        }

        private static class Mapping {
            @SerializedName("local_id")
            private Long localId;
            
            @SerializedName("server_id")
            private Long serverId;
        }
    }
} 