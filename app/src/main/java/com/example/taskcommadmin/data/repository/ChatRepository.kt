package com.example.taskcommadmin.data.repository

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
    
    suspend fun sendMessage(context: android.content.Context, message: ChatMessage): String? {
        return try {
            val postgrest = postgrest(context)
            Log.d("AdminChatRepo", "Sending message taskId=" + message.taskId + 
                ", senderId=" + message.senderId + ", role=" + message.senderRole + 
                ", textLen=" + (message.text.length))
            withContext(Dispatchers.IO) {
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
                postgrest["chat_messages"].update({
                    set("is_read", true)
                }) {
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
}

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
