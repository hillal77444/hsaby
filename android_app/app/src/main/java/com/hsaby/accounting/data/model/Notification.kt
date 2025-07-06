package com.hsaby.accounting.data.model

import java.util.Date

data class Notification(
    val id: Long = 0,
    val title: String,
    val message: String,
    val date: Date,
    val isRead: Boolean = false
) 