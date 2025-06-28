package com.hillal.acc.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "cashboxes")
public class Cashbox {
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    public int id;
    @SerializedName("name")
    public String name;
    @SerializedName("created_at")
    public String createdAt;
} 