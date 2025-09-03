package com.example.taskcommadmin.data.model

import com.google.firebase.Timestamp

data class Instruction(
    val instructionId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val status: String = "pending" // pending, in_progress, completed
)
