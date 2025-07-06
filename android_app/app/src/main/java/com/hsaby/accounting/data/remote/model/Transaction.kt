package com.hsaby.accounting.data.remote.model

import com.google.gson.annotations.SerializedName

data class Transaction(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("server_id")
    val serverId: Long? = null,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("type")
    val type: String, // "credit" (له) or "debit" (عليه)

    @SerializedName("description")
    val description: String,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("date")
    val date: Long,

    @SerializedName("currency")
    val currency: String = "SAR",

    @SerializedName("whatsapp_enabled")
    val whatsappEnabled: Boolean = false,

    @SerializedName("account_id")
    val accountId: Long,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("created_at")
    val createdAt: Long? = null,

    @SerializedName("updated_at")
    val updatedAt: Long? = null
) 