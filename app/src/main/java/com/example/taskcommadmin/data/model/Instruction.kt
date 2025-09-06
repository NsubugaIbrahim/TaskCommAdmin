package com.example.taskcommadmin.data.model

import com.example.taskcommadmin.data.constants.TaskStatus
import com.google.firebase.Timestamp

data class Instruction(
    val instructionId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val status: String = TaskStatus.PENDING // pending, in_progress, completed
)
