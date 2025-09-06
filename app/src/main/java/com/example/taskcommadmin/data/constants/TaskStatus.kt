package com.example.taskcommadmin.data.constants

object TaskStatus {
    const val PENDING = "pending"
    const val IN_PROGRESS = "in_progress"
    const val COMPLETED = "completed"
    
    val ALL_STATUSES = listOf(PENDING, IN_PROGRESS, COMPLETED)
    
    fun getStatusDisplayName(status: String): String {
        return when (status) {
            PENDING -> "Pending"
            IN_PROGRESS -> "In Progress"
            COMPLETED -> "Completed"
            else -> status.replaceFirstChar { it.uppercase() }
        }
    }
    
    fun getStatusEmoji(status: String): String {
        return when (status) {
            PENDING -> "⏳"
            IN_PROGRESS -> "🔄"
            COMPLETED -> "✅"
            else -> "📝"
        }
    }
}
