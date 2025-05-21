package com.hsaby.accounting.data.remote

import com.hsaby.accounting.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    companion object {
        const val BASE_URL = "http://212.224.88.122:5007/api/"
    }

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    @GET("accounts")
    suspend fun getAccounts(
        @Query("user_id") userId: String,
        @Query("last_sync_time") lastSyncTime: Long? = null
    ): Response<List<Account>>

    @POST("accounts")
    suspend fun createAccount(@Body account: Account): Response<Account>

    @PUT("accounts/{accountId}")
    suspend fun updateAccount(
        @Path("accountId") accountId: String,
        @Body account: Account
    ): Response<Account>

    @DELETE("accounts/{accountId}")
    suspend fun deleteAccount(@Path("accountId") accountId: String): Response<Unit>

    @GET("transactions")
    suspend fun getTransactions(
        @Query("user_id") userId: String,
        @Query("last_sync_time") lastSyncTime: Long? = null
    ): Response<List<Transaction>>

    @POST("transactions")
    suspend fun createTransaction(@Body transaction: Transaction): Response<Transaction>

    @PUT("transactions/{transactionId}")
    suspend fun updateTransaction(
        @Path("transactionId") transactionId: String,
        @Body transaction: Transaction
    ): Response<Transaction>

    @DELETE("transactions/{transactionId}")
    suspend fun deleteTransaction(@Path("transactionId") transactionId: String): Response<Unit>

    @POST("sync")
    suspend fun sync(@Body request: SyncRequest): Response<SyncResponse>

    @POST("sync/changes")
    suspend fun syncChanges(@Body request: SyncChangesRequest): Response<SyncChangesResponse>
} 