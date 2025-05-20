package com.hsaby.accounting.data.remote

import com.hsaby.accounting.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    companion object {
        const val BASE_URL = "http://212.224.88.122:5007/api/"
    }

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("accounts")
    suspend fun getAccounts(
        @Query("user_id") userId: String,
        @Query("last_sync_time") lastSyncTime: Long? = null
    ): Response<PaginatedResponse<Account>>

    @GET("transactions")
    suspend fun getTransactions(
        @Query("user_id") userId: String,
        @Query("last_sync_time") lastSyncTime: Long? = null
    ): Response<PaginatedResponse<Transaction>>

    @POST("sync")
    suspend fun syncData(@Body request: SyncRequest): Response<SyncResponse>

    @POST("sync/changes")
    suspend fun syncChanges(@Body request: SyncChangesRequest): Response<SyncChangesResponse>

    @POST("refresh-token")
    suspend fun refreshToken(): Response<RefreshTokenResponse>

    @DELETE("transactions/{transactionId}")
    suspend fun deleteTransaction(@Path("transactionId") transactionId: Long): Response<Unit>
} 