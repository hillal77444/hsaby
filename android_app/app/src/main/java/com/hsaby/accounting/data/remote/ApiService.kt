package com.hsaby.accounting.data.remote

import com.hsaby.accounting.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
    
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @GET("accounts")
    suspend fun getAccounts(): Response<List<Account>>
    
    @GET("transactions")
    suspend fun getTransactions(): Response<List<Transaction>>
    
    @POST("sync")
    suspend fun syncData(@Body request: SyncRequest): Response<SyncResponse>
    
    @POST("sync/changes")
    suspend fun syncChanges(@Body request: SyncChangesRequest): Response<SyncChangesResponse>
    
    @POST("refresh-token")
    suspend fun refreshToken(): Response<RefreshTokenResponse>
} 