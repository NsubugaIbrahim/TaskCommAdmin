package com.example.taskcommadmin.data.model

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val name: String = "",
    val address: String = "",
    val businessField: String = "",
    val email: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true
)
