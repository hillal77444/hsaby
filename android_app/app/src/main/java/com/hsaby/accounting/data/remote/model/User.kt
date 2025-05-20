package com.hsaby.accounting.data.remote.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("created_at")
    val createdAt: Long? = null,

    @SerializedName("updated_at")
    val updatedAt: Long? = null
)

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("user")
    val user: User
)

data class RegisterResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("user")
    val user: User
) 