package com.example.taskcommadmin.data.repository

import android.annotation.SuppressLint
import com.example.taskcommadmin.data.SupabaseClientProvider
import com.example.taskcommadmin.data.model.ChatMessage
import com.google.firebase.Timestamp
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.InputStream

class ChatRepository {
    private suspend fun postgrest(context: android.content.Context): Postgrest {
        val client = SupabaseClientProvider.getClient(context)
        return client.pluginManager.getPlugin(Postgrest)
    }
    
    // Comprehensive diagnostic function for RLS and permissions
    suspend fun diagnosePermissions(context: android.content.Context, messageId: String): String {
        return try {
            val postgrest = postgrest(context)
            val client = SupabaseClientProvider.getClient(context)
            val auth = client.pluginManager.getPlugin(io.github.jan.supabase.gotrue.Auth)
            
            val diagnostics = StringBuilder()
            
            withContext(Dispatchers.IO) {
                // 1. Check authentication
                val session = auth.currentSessionOrNull()
                diagnostics.appendLine("=== AUTHENTICATION DIAGNOSTICS ===")
                diagnostics.appendLine("User ID: ${session?.user?.id}")
                diagnostics.appendLine("User Email: ${session?.user?.email}")
                diagnostics.appendLine("User Role: ${session?.user?.appMetadata?.get("role")}")
                diagnostics.appendLine("Session Valid: ${session != null}")
                diagnostics.appendLine()
                
                // 2. Check if we can read the specific message
                diagnostics.appendLine("=== MESSAGE ACCESS DIAGNOSTICS ===")
                try {
                    val message = postgrest["chat_messages"].select {
                        filter { eq("id", messageId) }
                        limit(1)
                    }.decodeList<ChatRow>()
                    
                    if (message.isNotEmpty()) {
                        val msg = message.first()
                        diagnostics.appendLine("Message found: YES")
                        diagnostics.appendLine("Message ID: ${msg.id}")
                        diagnostics.appendLine("Task ID: ${msg.taskId}")
                        diagnostics.appendLine("Sender ID: ${msg.senderId}")
                        diagnostics.appendLine("Sender Role: ${msg.senderRole}")
                        diagnostics.appendLine("Text: ${msg.text}")
                        diagnostics.appendLine("Created: ${msg.createdAt}")
                    } else {
                        diagnostics.appendLine("Message found: NO")
                    }
                } catch (e: Exception) {
                    diagnostics.appendLine("Message read error: ${e.message}")
                }
                diagnostics.appendLine()
                
                // 3. Check RLS policies by trying different operations
                diagnostics.appendLine("=== RLS POLICY DIAGNOSTICS ===")
                
                // Try to read all messages (should be restricted by RLS)
                try {
                    val allMessages = postgrest["chat_messages"].select {
                        limit(10)
                    }.decodeList<ChatRow>()
                    diagnostics.appendLine("Can read all messages: YES (${allMessages.size} messages)")
                } catch (e: Exception) {
                    diagnostics.appendLine("Can read all messages: NO - ${e.message}")
                }
                
                // Try to read messages from same task
                try {
                    val message = postgrest["chat_messages"].select {
                        filter { eq("id", messageId) }
                        limit(1)
                    }.decodeList<ChatRow>()
                    
                    if (message.isNotEmpty()) {
                        val taskMessages = postgrest["chat_messages"].select {
                            filter { eq("task_id", message.first().taskId ?: "") }
                        }.decodeList<ChatRow>()
                        diagnostics.appendLine("Can read task messages: YES (${taskMessages.size} messages)")
                    }
                } catch (e: Exception) {
                    diagnostics.appendLine("Can read task messages: NO - ${e.message}")
                }
                
                // Try to read messages from same sender
                try {
                    val message = postgrest["chat_messages"].select {
                        filter { eq("id", messageId) }
                        limit(1)
                    }.decodeList<ChatRow>()
                    
                    if (message.isNotEmpty()) {
                        val senderMessages = postgrest["chat_messages"].select {
                            filter { eq("sender_id", message.first().senderId ?: "") }
                        }.decodeList<ChatRow>()
                        diagnostics.appendLine("Can read sender messages: YES (${senderMessages.size} messages)")
                    }
                } catch (e: Exception) {
                    diagnostics.appendLine("Can read sender messages: NO - ${e.message}")
                }
                diagnostics.appendLine()
                
                // 4. Test write permissions
                diagnostics.appendLine("=== WRITE PERMISSION DIAGNOSTICS ===")
                
                // Try to insert a test message
                try {
                    val testMessage = ChatInsertRow(
                        taskId = "test-task-${System.currentTimeMillis()}",
                        senderId = session?.user?.id ?: "test-sender",
                        senderRole = "admin",
                        text = "Test message for write permission",
                        fileType = "text"
                    )
                    
                    val insertResult = postgrest["chat_messages"].insert(testMessage)
                    diagnostics.appendLine("Can insert messages: YES")
                    
                    // Clean up test message
                    try {
                        postgrest["chat_messages"].delete {
                            filter { eq("text", "Test message for write permission") }
                        }
                        diagnostics.appendLine("Can delete test messages: YES")
                    } catch (e: Exception) {
                        diagnostics.appendLine("Can delete test messages: NO - ${e.message}")
                    }
                } catch (e: Exception) {
                    diagnostics.appendLine("Can insert messages: NO - ${e.message}")
                }
                
                // Try to update the specific message
                try {
                    val updateResult = postgrest["chat_messages"].update(mapOf(
                        "text" to "Test update for permission check"
                    )) {
                        filter { eq("id", messageId) }
                    }
                    diagnostics.appendLine("Can update specific message: YES")
                    
                    // Revert the change
                    postgrest["chat_messages"].update(mapOf(
                        "text" to "Original message text"
                    )) {
                        filter { eq("id", messageId) }
                    }
                } catch (e: Exception) {
                    diagnostics.appendLine("Can update specific message: NO - ${e.message}")
                }
                
                // Try to delete the specific message
                try {
                    val deleteResult = postgrest["chat_messages"].delete {
                        filter { eq("id", messageId) }
                    }
                    diagnostics.appendLine("Can delete specific message: YES")
                } catch (e: Exception) {
                    diagnostics.appendLine("Can delete specific message: NO - ${e.message}")
                }
            }
            
            diagnostics.toString()
        } catch (e: Exception) {
            "Diagnostic failed: ${e.message}\n${e.stackTraceToString()}"
        }
    }
    
    // Test function to check table access
    suspend fun testTableAccess(context: android.content.Context): Boolean {
        return try {
            val postgrest = postgrest(context)
            withContext(Dispatchers.IO) {
                // Check authentication first
                try {
                    val client = SupabaseClientProvider.getClient(context)
                    val auth = client.pluginManager.getPlugin(io.github.jan.supabase.gotrue.Auth)
                    val session = auth.currentSessionOrNull()
                    Log.d("AdminChatRepo", "Auth check: session=${session?.user?.id}, role=${session?.user?.appMetadata?.get("role")}")
                } catch (e: Exception) {
                    Log.e("AdminChatRepo", "Auth check failed: ${e.message}")
                }
                
                // Try to read
                val readResult = postgrest["chat_messages"].select {
                    limit(1)
                }.decodeList<ChatRow>()
                Log.d("AdminChatRepo", "Read test passed: ${readResult.size} rows")
                
                // Check table structure
                if (readResult.isNotEmpty()) {
                    val firstRow = readResult.first()
                    Log.d("AdminChatRepo", "Table columns: id=${firstRow.id != null}, taskId=${firstRow.taskId != null}, senderId=${firstRow.senderId != null}, text=${firstRow.text != null}")
                } else {
                    Log.d("AdminChatRepo", "No rows found for table structure check")
                }
                
                // Try to write a test message
                val testMessage = ChatInsertRow(
                    taskId = "test-task",
                    senderId = "test-sender",
                    senderRole = "admin",
                    text = "Test message for table access",
                    fileType = "text"
                )
                
                val insertResult = postgrest["chat_messages"].insert(testMessage)
                Log.d("AdminChatRepo", "Write test passed: $insertResult")
                
                // Clean up test message
                postgrest["chat_messages"].delete {
                    filter { eq("text", "Test message for table access") }
                }
                Log.d("AdminChatRepo", "Cleanup test passed")
                
                true
            }
        } catch (e: Exception) {
            Log.e("AdminChatRepo", "Table access test failed: ${e.message}")
            false
        }
    }

    fun getMessagesByTask(context: android.content.Context, taskId: String): Flow<List<ChatMessage>> = flow {
        val postgrest = postgrest(context)
        while (true) {
            try {
                Log.d("AdminChatRepo", "Polling messages for task=" + taskId)
                val rows = withContext(Dispatchers.IO) {
                    postgrest["chat_messages"].select {
                        filter { eq("task_id", taskId) }
                        order(column = "created_at", order = Order.ASCENDING)
                    }.decodeList<ChatRow>()
                }
                Log.d("AdminChatRepo", "Fetched rows count=" + rows.size)
                emit(rows.map { it.toModel() })
            } catch (_: Exception) {
                Log.e("AdminChatRepo", "Fetch failed: ")
                emit(emptyList())
            }
            delay(1500)
        }
    }
    
    // Smart polling that respects local changes
    fun getMessagesByTaskWithLocalChanges(
        context: android.content.Context, 
        taskId: String,
        localMessages: List<ChatMessage>
    ): Flow<List<ChatMessage>> {
        return flow {
            while (true) {
                try {
                    val postgrest = postgrest(context)
                    val rows = withContext(Dispatchers.IO) {
                        postgrest["chat_messages"].select {
                            filter { eq("task_id", taskId) }
                            order(column = "created_at", order = Order.ASCENDING)
                        }.decodeList<ChatRow>()
                    }
                    
                    // Merge server data with local optimistic changes
                    val serverMessages = rows.map { it.toModel() }
                    val mergedMessages = mergeLocalAndServerMessages(localMessages, serverMessages)
                    
                    Log.d("AdminChatRepo", "Merged: server=${serverMessages.size}, local=${localMessages.size}, final=${mergedMessages.size}")
                    emit(mergedMessages)
                } catch (_: Exception) {
                    Log.e("AdminChatRepo", "Fetch failed: ")
                    emit(localMessages) // Keep local changes if fetch fails
                }
                delay(1500)
            }
        }
    }
    
    private fun mergeLocalAndServerMessages(
        localMessages: List<ChatMessage>, 
        serverMessages: List<ChatMessage>
    ): List<ChatMessage> {
        val localMap = localMessages.associateBy { it.messageId }.toMutableMap()
        val serverMap = serverMessages.associateBy { it.messageId }
        
        val merged = mutableListOf<ChatMessage>()
        
        // Add all server messages
        serverMessages.forEach { serverMsg ->
            val localMsg = localMap[serverMsg.messageId]
            if (localMsg != null && localMsg.text.endsWith("")) {
                // Keep local edited version
                merged.add(localMsg)
            } else {
                merged.add(serverMsg)
                // Remove from local map to avoid duplicates
                localMap.remove(serverMsg.messageId)
            }
        }
        
        // Add any local messages that don't exist on server (optimistic sends)
        localMessages.forEach { localMsg ->
            if (!serverMap.containsKey(localMsg.messageId)) {
                merged.add(localMsg)
            }
        }
        
        return merged.sortedBy { it.timestamp }
    }
    
    suspend fun sendMessage(context: android.content.Context, message: ChatMessage): String? {
        return try {
            val postgrest = postgrest(context)
            Log.d("AdminChatRepo", "Sending message taskId=" + message.taskId + 
                ", senderId=" + message.senderId + ", role=" + message.senderRole + 
                ", textLen=" + (message.text.length))
            withContext(Dispatchers.IO) {
                // Check table structure first
                try {
                    val testSelect = postgrest["chat_messages"].select {
                        limit(1)
                    }.decodeList<ChatRow>()
                    Log.d("AdminChatRepo", "Table structure check passed, columns: ${testSelect.firstOrNull()?.let { it::class.simpleName } ?: "unknown"}")
                    
                    // Check if we can see the message we're trying to send
                    val existingMessages = postgrest["chat_messages"].select {
                        filter { eq("task_id", message.taskId) }
                        limit(5)
                    }.decodeList<ChatRow>()
                    Log.d("AdminChatRepo", "Existing messages for task: ${existingMessages.size}")
                } catch (e: Exception) {
                    Log.e("AdminChatRepo", "Table structure check failed: ${e.message}")
                }
                
                postgrest["chat_messages"].insert(
                    ChatInsertRow(
                        taskId = message.taskId,
                        senderId = message.senderId,
                        senderRole = message.senderRole,
                        text = message.text,
                        mediaUrl = message.mediaUrl,
                        fileType = message.fileType ?: "text",
                        fileName = message.fileName
                    )
                )
            }
            Log.d("AdminChatRepo", "Send success")
            ""
        } catch (e: Exception) {
            Log.e("AdminChatRepo", "Send failed: " + (e.message ?: "error"))
            null
        }
    }
    
    // Optional: Switch to Supabase Storage if needed. Keeping placeholder for now.
    suspend fun uploadFile(
        taskId: String,
        fileName: String,
        inputStream: InputStream,
        fileType: String
    ): String? {
        return null
    }
    
    suspend fun markMessageAsRead(context: android.content.Context, messageId: String): Boolean {
        return try {
            val postgrest = postgrest(context)
            withContext(Dispatchers.IO) {
                val result = postgrest["chat_messages"].update(mapOf(
                    "is_read" to true
                )) {
                    filter { eq("id", messageId) }
                }
            }
            Log.d("AdminChatRepo", "Marked read messageId=" + messageId)
            true
        } catch (e: Exception) {
            Log.e("AdminChatRepo", "Mark read failed: " + (e.message ?: "error"))
            false
        }
    }
    
    suspend fun getUnreadMessageCount(context: android.content.Context, taskId: String): Int {
        return try {
            val postgrest = postgrest(context)
            val rows = withContext(Dispatchers.IO) {
                postgrest["chat_messages"].select {
                    filter { eq("task_id", taskId) }
                    filter { eq("is_read", false) }
                    order(column = "created_at", order = Order.ASCENDING)
                }.decodeList<ChatRow>()
            }
            Log.d("AdminChatRepo", "Unread count for task=" + taskId + " = " + rows.size)
            rows.size
        } catch (e: Exception) {
            Log.e("AdminChatRepo", "Unread fetch failed: " + (e.message ?: "error"))
            0
        }
    }
    
    suspend fun deleteMessage(context: android.content.Context, messageId: String): Boolean {
        return try {
            val postgrest = postgrest(context)
            withContext(Dispatchers.IO) {
                Log.d("AdminChatRepo", "Starting delete operation for message: $messageId")
                
                // First check if message exists and get details
                val existingMessage = postgrest["chat_messages"].select {
                    filter { eq("id", messageId) }
                    limit(1)
                }.decodeList<ChatRow>()
                
                if (existingMessage.isEmpty()) {
                    Log.w("AdminChatRepo", "Message not found for deletion: $messageId")
                    return@withContext true // Already deleted
                }
                
                val message = existingMessage.first()
                Log.d("AdminChatRepo", "Found message for deletion: $messageId")
                Log.d("AdminChatRepo", "Message details: taskId=${message.taskId}, senderId=${message.senderId}, senderRole=${message.senderRole}")
                
                // Check authentication and permissions
                try {
                    val client = SupabaseClientProvider.getClient(context)
                    val auth = client.pluginManager.getPlugin(io.github.jan.supabase.gotrue.Auth)
                    val session = auth.currentSessionOrNull()
                    Log.d("AdminChatRepo", "Current user: ${session?.user?.id}, role: ${session?.user?.appMetadata?.get("role")}")
                } catch (e: Exception) {
                    Log.e("AdminChatRepo", "Auth check failed: ${e.message}")
                }
                
                // Attempt the delete operation
                Log.d("AdminChatRepo", "Attempting to delete message: $messageId")
                val result = postgrest["chat_messages"].delete {
                    filter { eq("id", messageId) }
                }
                Log.d("AdminChatRepo", "Delete operation completed: $result")
                
                // Wait a bit for the operation to be processed
                kotlinx.coroutines.delay(200)
                
                // Verify deletion by trying to fetch the message
                val verifyResult = postgrest["chat_messages"].select {
                    filter { eq("id", messageId) }
                    limit(1)
                }.decodeList<ChatRow>()
                
                if (verifyResult.isNotEmpty()) {
                    Log.e("AdminChatRepo", "Delete verification failed: message still exists: $messageId")
                    Log.e("AdminChatRepo", "Remaining message text: ${verifyResult.first().text}")
                    return@withContext false
                } else {
                    Log.d("AdminChatRepo", "Delete verification successful: message no longer exists")
                }
                
                // Additional verification: check if message is gone from task messages
                val taskMessages = postgrest["chat_messages"].select {
                    filter { eq("task_id", message.taskId ?: "") }
                }.decodeList<ChatRow>()
                val messageStillInTask = taskMessages.any { it.id == messageId }
                if (messageStillInTask) {
                    Log.e("AdminChatRepo", "Message still appears in task messages after delete")
                    return@withContext false
                } else {
                    Log.d("AdminChatRepo", "Message successfully removed from task messages")
                }
            }
            Log.d("AdminChatRepo", "Successfully deleted message: $messageId")
            true
        } catch (e: Exception) {
            Log.e("AdminChatRepo", "Delete operation failed: ${e.message}")
            Log.e("AdminChatRepo", "Exception type: ${e::class.simpleName}")
            e.printStackTrace()
            false
        }
    }
    
    suspend fun editMessage(context: android.content.Context, messageId: String, newText: String): Boolean {
        return try {
            val postgrest = postgrest(context)
            withContext(Dispatchers.IO) {
                Log.d("AdminChatRepo", "Starting edit operation for message: $messageId")
                Log.d("AdminChatRepo", "New text: $newText")
                
                // First check if message exists and get current text
                val existingMessage = postgrest["chat_messages"].select {
                    filter { eq("id", messageId) }
                    limit(1)
                }.decodeList<ChatRow>()
                
                if (existingMessage.isEmpty()) {
                    Log.w("AdminChatRepo", "Message not found for edit: $messageId")
                    return@withContext false
                }
                
                val message = existingMessage.first()
                Log.d("AdminChatRepo", "Found message for edit: $messageId")
                Log.d("AdminChatRepo", "Current text: ${message.text}")
                Log.d("AdminChatRepo", "Message details: taskId=${message.taskId}, senderId=${message.senderId}, senderRole=${message.senderRole}")
                
                // Check authentication and permissions
                try {
                    val client = SupabaseClientProvider.getClient(context)
                    val auth = client.pluginManager.getPlugin(io.github.jan.supabase.gotrue.Auth)
                    val session = auth.currentSessionOrNull()
                    Log.d("AdminChatRepo", "Current user: ${session?.user?.id}, role: ${session?.user?.appMetadata?.get("role")}")
                } catch (e: Exception) {
                    Log.e("AdminChatRepo", "Auth check failed: ${e.message}")
                }
                
                // Attempt the update operation
                Log.d("AdminChatRepo", "Attempting to update message: $messageId")
                val textWithEditedFlag = newText + " (edited)"
                val result = postgrest["chat_messages"].update(mapOf(
                    "text" to textWithEditedFlag
                )) {
                    filter { eq("id", messageId) }
                }
                Log.d("AdminChatRepo", "Update operation completed: $result")
                
                // Wait a bit for the operation to be processed
                kotlinx.coroutines.delay(200)
                
                // Verify edit by fetching the message
                val verifyResult = postgrest["chat_messages"].select {
                    filter { eq("id", messageId) }
                    limit(1)
                }.decodeList<ChatRow>()
                
                if (verifyResult.isEmpty()) {
                    Log.e("AdminChatRepo", "Edit verification failed: message no longer exists: $messageId")
                    return@withContext false
                }
                
                val updatedMessage = verifyResult.first()
                if (updatedMessage.text != textWithEditedFlag) {
                    Log.e("AdminChatRepo", "Edit verification failed: text not updated")
                    Log.e("AdminChatRepo", "Expected: $textWithEditedFlag")
                    Log.e("AdminChatRepo", "Actual: ${updatedMessage.text}")
                    return@withContext false
                } else {
                    Log.d("AdminChatRepo", "Edit verification successful: text updated correctly")
                }
                
                // Additional verification: check if message appears correctly in task messages
                val taskMessages = postgrest["chat_messages"].select {
                    filter { eq("task_id", message.taskId ?: "") }
                }.decodeList<ChatRow>()
                val updatedMessageInTask = taskMessages.find { it.id == messageId }
                if (updatedMessageInTask?.text != newText) {
                    Log.e("AdminChatRepo", "Message not updated correctly in task messages")
                    return@withContext false
                } else {
                    Log.d("AdminChatRepo", "Message successfully updated in task messages")
                }
            }
            Log.d("AdminChatRepo", "Successfully edited message: $messageId")
            true
        } catch (e: Exception) {
            Log.e("AdminChatRepo", "Edit operation failed: ${e.message}")
            Log.e("AdminChatRepo", "Exception type: ${e::class.simpleName}")
            e.printStackTrace()
            false
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
private data class ChatRow(
    @SerialName("id") val id: String? = null,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("sender_role") val senderRole: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_read") val isRead: Boolean? = null
) {
    fun toModel(): ChatMessage {
        val ts = try {
            val odt = java.time.OffsetDateTime.parse(createdAt)
            Timestamp(java.util.Date.from(odt.toInstant()))
        } catch (_: Exception) { Timestamp.now() }
        return ChatMessage(
            messageId = id ?: "",
            taskId = taskId ?: "",
            senderId = senderId ?: "",
            senderRole = senderRole ?: "",
            senderName = "",
            text = text ?: "",
            mediaUrl = mediaUrl,
            fileType = fileType,
            fileName = fileName,
            timestamp = ts,
            isRead = isRead ?: false
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
private data class ChatInsertRow(
    @SerialName("task_id") val taskId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_role") val senderRole: String,
    @SerialName("text") val text: String,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("file_type") val fileType: String = "text",
    @SerialName("file_name") val fileName: String? = null
)
