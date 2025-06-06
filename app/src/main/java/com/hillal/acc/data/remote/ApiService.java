package com.hillal.acc.data.remote;

import com.hillal.acc.data.model.User;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.data.room.AppDatabase;
import com.hillal.acc.data.room.AccountDao;

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
import android.util.Log;
import android.content.Context;
import com.hillal.acc.App;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

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
            ExecutorService executor = Executors.newSingleThreadExecutor();
            
            for (Transaction transaction : transactions) {
                try {
                    // البحث عن الحساب في قاعدة البيانات المحلية
                    AppDatabase db = AppDatabase.getInstance(transaction.getContext());
                    AccountDao accountDao = db.accountDao();
                    
                    // استخدام Executor للتعامل مع قاعدة البيانات
                    Future<Account> future = executor.submit(new Callable<Account>() {
                        @Override
                        public Account call() {
                            return accountDao.getAccountByIdSync(transaction.getAccountId());
                        }
                    });
                    
                    Account relatedAccount = future.get(); // انتظار النتيجة
                    
                    if (relatedAccount == null || relatedAccount.getServerId() <= 0) {
                        Log.d("SyncRequest", "تخطي المعاملة - معرف الحساب غير صالح: " + transaction.getAccountId());
                        continue;
                    }

                    Log.d("SyncRequest", String.format("تحويل معرف الحساب: المحلي=%d, الخادم=%d", 
                        transaction.getAccountId(), relatedAccount.getServerId()));

                    Transaction newTransaction = new Transaction();
                    newTransaction.setId(transaction.getId());
                    newTransaction.setServerId(transaction.getServerId());
                    newTransaction.setUserId(transaction.getUserId());
                    newTransaction.setAccountId(relatedAccount.getServerId()); // استخدام معرف الخادم للحساب
                    
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
                } catch (Exception e) {
                    Log.e("SyncRequest", "خطأ في معالجة المعاملة: " + e.getMessage());
                }
            }
            
            executor.shutdown();
            return transactionsToSend;
        }
    }

    public static class SyncResponse {
        @SerializedName("message")
        private String message;
        
        @SerializedName("account_mappings")
        private List<Map<String, Long>> account_mappings;
        
        @SerializedName("transaction_mappings")
        private List<Map<String, Long>> transaction_mappings;

        public String getMessage() {
            return message;
        }

        public List<Map<String, Long>> getAccountMappings() {
            return account_mappings;
        }

        public List<Map<String, Long>> getTransactionMappings() {
            return transaction_mappings;
        }

        public Long getAccountServerId(Long localId) {
            if (account_mappings != null) {
                for (Map<String, Long> mapping : account_mappings) {
                    Long local_id = mapping.get("local_id");
                    if (local_id != null && local_id.equals(localId)) {
                        Long server_id = mapping.get("server_id");
                        if (server_id != null && server_id > 0) {
                            return server_id;
                        }
                    }
                }
            }
            return null;
        }

        public Long getTransactionServerId(Long localId) {
            if (transaction_mappings != null) {
                for (Map<String, Long> mapping : transaction_mappings) {
                    Long local_id = mapping.get("local_id");
                    if (local_id != null && local_id.equals(localId)) {
                        Long server_id = mapping.get("server_id");
                        if (server_id != null && server_id > 0) {
                            return server_id;
                        }
                    }
                }
            }
            return null;
        }

        // إضافة دالة للتحقق من صحة الرد
        public boolean isValid() {
            return account_mappings != null && transaction_mappings != null;
        }

        // إضافة دالة للحصول على تفاصيل الرد
        public String getResponseDetails() {
            StringBuilder details = new StringBuilder();
            details.append("Message: ").append(message).append("\n");
            
            if (account_mappings != null) {
                details.append("Account Mappings:\n");
                for (Map<String, Long> mapping : account_mappings) {
                    details.append("  Local ID: ").append(mapping.get("local_id"))
                           .append(" -> Server ID: ").append(mapping.get("server_id"))
                           .append("\n");
                }
            }
            
            if (transaction_mappings != null) {
                details.append("Transaction Mappings:\n");
                for (Map<String, Long> mapping : transaction_mappings) {
                    details.append("  Local ID: ").append(mapping.get("local_id"))
                           .append(" -> Server ID: ").append(mapping.get("server_id"))
                           .append("\n");
                }
            }
            
            return details.toString();
        }
    }
} 