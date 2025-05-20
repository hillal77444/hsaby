package com.hsaby.accounting.data.remote.model

import com.google.gson.annotations.SerializedName

data class Account(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("server_id")
    val serverId: Long? = null,

    @SerializedName("account_name")
    val accountName: String,

    @SerializedName("balance")
    val balance: Double,

    @SerializedName("phone_number")
    val phoneNumber: String? = null,

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("is_debtor")
    val isDebtor: Boolean = false,

    @SerializedName("whatsapp_enabled")
    val whatsappEnabled: Boolean = false,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("created_at")
    val createdAt: Long? = null,

    @SerializedName("updated_at")
    val updatedAt: Long? = null
) 