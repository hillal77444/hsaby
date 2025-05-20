package com.hsaby.accounting.data.model

data class PaginatedResponse<T>(
    val data: List<T>,
    val totalPages: Int,
    val currentPage: Int,
    val totalItems: Int
) 