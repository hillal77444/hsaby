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
    public long id;
    @SerializedName("name")
    public String name;
    @SerializedName("created_at")
    public String createdAt;

    public long getId() { return id; }
    public String getName() { return name; }
} 