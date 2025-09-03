package com.example.taskcommadmin.data.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val messageId: String = "",
    val taskId: String = "",
    val senderId: String = "",
    val senderRole: String = "", // user, admin
    val senderName: String = "",
    val text: String = "",
    val mediaUrl: String? = null,
    val fileType: String? = null, // image, document, text
    val fileName: String? = null,
    val fileSize: Long? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)
