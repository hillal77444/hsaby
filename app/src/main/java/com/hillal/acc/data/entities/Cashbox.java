package com.hillal.acc.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "Cashbox")
public class Cashbox {
    @PrimaryKey
    @NonNull
    public int id;
    public String name;
    public String createdAt;
} 