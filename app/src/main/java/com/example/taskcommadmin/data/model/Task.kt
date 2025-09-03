package com.example.taskcommadmin.data.model

import com.google.firebase.Timestamp

data class Task(
    val taskId: String = "",
    val instructionId: String = "",
    val adminId: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "pending", // pending, in_progress, completed
    val priority: String = "medium", // low, medium, high
    val dueDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
