package com.accounting.app.api

import com.accounting.app.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/register")
    suspend fun register(@Body user: User): Response<ApiResponse<User>>

    @POST("api/login")
    suspend fun login(@Body credentials: LoginRequest): Response<ApiResponse<LoginResponse>>

    @GET("api/accounts")
    suspend fun getAccounts(@Header("Authorization") token: String): Response<ApiResponse<List<Account>>>

    @POST("api/accounts")
    suspend fun addAccount(
        @Header("Authorization") token: String,
        @Body account: Account
    ): Response<ApiResponse<Account>>

    @PUT("api/accounts/{id}")
    suspend fun updateAccount(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body account: Account
    ): Response<ApiResponse<Account>>

    @DELETE("api/accounts/{id}")
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>

    @GET("api/transactions")
    suspend fun getTransactions(
        @Header("Authorization") token: String,
        @Query("account_id") accountId: Long? = null
    ): Response<ApiResponse<List<Transaction>>>

    @POST("api/transactions")
    suspend fun addTransaction(
        @Header("Authorization") token: String,
        @Body transaction: Transaction
    ): Response<ApiResponse<Transaction>>

    @PUT("api/transactions/{id}")
    suspend fun updateTransaction(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body transaction: Transaction
    ): Response<ApiResponse<Transaction>>

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>

    @POST("api/sync")
    suspend fun syncData(
        @Header("Authorization") token: String,
        @Body syncData: SyncData
    ): Response<ApiResponse<SyncResponse>>
} 