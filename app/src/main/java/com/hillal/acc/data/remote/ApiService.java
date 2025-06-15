package com.hillal.acc.data.remote;

import com.hillal.acc.data.model.User;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.data.model.ServerAppUpdateInfo;

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
    Call<List<Transaction>> getTransactions(
        @Header("Authorization") String token,
        @Query("limit") int limit,
        @Query("offset") int offset
    );

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

    @POST("api/update_user_details")
    Call<Map<String, String>> updateUserDetails(
        @Header("Authorization") String token,
        @Body com.google.gson.JsonObject requestBody
    );

    @GET("/api/app/updates/check")
    Call<ServerAppUpdateInfo> checkForUpdates(@Header("Authorization") String token);

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
            // نسخ الحسابات مع جميع البيانات المطلوبة
            List<Account> accountsToSend = new ArrayList<>();
            for (Account account : accounts) {
                Account newAccount = new Account();
                newAccount.setId(account.getId());
                newAccount.setServerId(account.getServerId());
                newAccount.setAccountNumber(account.getAccountNumber());
                newAccount.setName(account.getName());
                newAccount.setBalance(account.getBalance());
                newAccount.setIsDebtor(account.isDebtor());
                newAccount.setPhoneNumber(account.getPhoneNumber());
                newAccount.setNotes(account.getNotes());
                newAccount.setWhatsappEnabled(account.isWhatsappEnabled());
                newAccount.setUserId(account.getUserId());
                newAccount.setCreatedAt(account.getCreatedAt());
                newAccount.setUpdatedAt(account.getUpdatedAt());
                accountsToSend.add(newAccount);
            }
            return accountsToSend;
        }

        public List<Transaction> getTransactions() {
            // نسخ المعاملات مع last_sync_time
            List<Transaction> transactionsToSend = new ArrayList<>();
            for (Transaction transaction : transactions) {
                Transaction newTransaction = new Transaction();
                newTransaction.setId(transaction.getId());
                newTransaction.setServerId(transaction.getServerId());
                newTransaction.setUserId(transaction.getUserId());
                
                // البحث عن الحساب المرتبط بالمعاملة
                Account relatedAccount = accounts.stream()
                    .filter(acc -> acc.getId() == transaction.getAccountId())
                    .findFirst()
                    .orElse(null);
                
                if (relatedAccount != null && relatedAccount.getServerId() > 0) {
                    newTransaction.setAccountId(relatedAccount.getServerId());
                } else {
                    newTransaction.setAccountId(transaction.getAccountId());
                }
                
                newTransaction.setAmount(transaction.getAmount());
                newTransaction.setType(transaction.getType());
                newTransaction.setDescription(transaction.getDescription());
                newTransaction.setNotes(transaction.getNotes());
                newTransaction.setCurrency(transaction.getCurrency());
                newTransaction.setTransactionDate(transaction.getTransactionDate());
                newTransaction.setCreatedAt(transaction.getCreatedAt());
                newTransaction.setUpdatedAt(transaction.getUpdatedAt());
                newTransaction.setModified(transaction.isModified());
                newTransaction.setWhatsappEnabled(transaction.isWhatsappEnabled());
                newTransaction.setSyncStatus(transaction.getSyncStatus());
                transactionsToSend.add(newTransaction);
            }
            return transactionsToSend;
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

        public static class Mapping {
            @SerializedName("local_id")
            public Long localId;
            
            @SerializedName("server_id")
            public Long serverId;
        }
    }
} 
