package com.hsaby.accounting.data.model

import com.google.gson.annotations.SerializedName

// Request Models
data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

data class LoginRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

data class SyncRequest(
    @SerializedName("accounts") val accounts: List<Account>,
    @SerializedName("transactions") val transactions: List<Transaction>
)

data class SyncChangesRequest(
    @SerializedName("last_sync") val lastSync: Long
)

// Response Models
data class RegisterResponse(
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String
)

data class LoginResponse(
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String
)

data class SyncResponse(
    @SerializedName("accounts") val accounts: List<Account>,
    @SerializedName("transactions") val transactions: List<Transaction>
)

data class SyncChangesResponse(
    @SerializedName("accounts") val accounts: List<Account>,
    @SerializedName("transactions") val transactions: List<Transaction>
)

data class RefreshTokenResponse(
    @SerializedName("token") val token: String
)

data class PaginatedResponse<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("total_items") val totalItems: Int
)

// Data Models
data class Account(
    @SerializedName("id") val id: String,
    @SerializedName("server_id") val serverId: Long,
    @SerializedName("account_name") val accountName: String,
    @SerializedName("balance") val balance: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("is_debtor") val isDebtor: Boolean,
    @SerializedName("whatsapp_enabled") val whatsappEnabled: Boolean,
    @SerializedName("user_id") val userId: String,
    @SerializedName("last_sync") val lastSync: Long
)

data class Transaction(
    @SerializedName("id") val id: String,
    @SerializedName("server_id") val serverId: Long,
    @SerializedName("account_id") val accountId: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String,
    @SerializedName("date") val date: Long,
    @SerializedName("currency") val currency: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("whatsapp_enabled") val whatsappEnabled: Boolean,
    @SerializedName("user_id") val userId: String,
    @SerializedName("is_synced") val isSynced: Boolean,
    @SerializedName("last_sync") val lastSync: Long
) 