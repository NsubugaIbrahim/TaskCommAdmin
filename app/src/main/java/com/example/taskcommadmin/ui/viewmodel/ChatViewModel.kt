package com.example.taskcommadmin.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskcommadmin.data.model.ChatMessage
import com.example.taskcommadmin.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import android.util.Log

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var currentTaskId: String? = null
    
    fun loadMessages(context: android.content.Context, taskId: String) {
        currentTaskId = taskId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Test table access first
                val tableAccessOk = chatRepository.testTableAccess(context)
                Log.d("AdminChatVM", "Table access test result: $tableAccessOk")
                
                chatRepository.getMessagesByTaskWithLocalChanges(context, taskId, _messages.value ?: emptyList()).collect { messageList ->
                    Log.d("AdminChatVM", "Emitting messages: count=" + messageList.size)
                    _messages.value = messageList
                }
            } catch (e: Exception) {
                Log.e("AdminChatVM", "loadMessages error: " + (e.message ?: "error"))
                _error.value = e.message ?: "Failed to load messages"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendTextMessage(
        context: android.content.Context,
        taskId: String,
        text: String,
        senderId: String,
        senderName: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val message = ChatMessage(
                    taskId = taskId,
                    senderId = senderId,
                    senderRole = "admin",
                    senderName = senderName,
                    text = text,
                    fileType = "text"
                )
                
                val messageId = chatRepository.sendMessage(context, message)
                if (messageId == null) {
                    Log.e("AdminChatVM", "sendTextMessage failed")
                    _error.value = "Failed to send message"
                }
            } catch (e: Exception) {
                Log.e("AdminChatVM", "sendTextMessage exception: " + (e.message ?: "error"))
                _error.value = e.message ?: "Send message failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendFileMessage(
        context: android.content.Context,
        taskId: String,
        inputStream: InputStream,
        fileName: String,
        fileType: String,
        senderId: String,
        senderName: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val mediaUrl = chatRepository.uploadFile(taskId, fileName, inputStream, fileType)
                if (mediaUrl != null) {
                    val message = ChatMessage(
                        taskId = taskId,
                        senderId = senderId,
                        senderRole = "admin",
                        senderName = senderName,
                        text = "File: $fileName",
                        mediaUrl = mediaUrl,
                        fileType = fileType,
                        fileName = fileName
                    )
                    
                    val messageId = chatRepository.sendMessage(context, message)
                    if (messageId == null) {
                        _error.value = "Failed to send file message"
                    }
                } else {
                    _error.value = "Failed to upload file"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Send file failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun markMessageAsRead(context: android.content.Context, messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.markMessageAsRead(context, messageId)
            } catch (e: Exception) {
                // Silent fail for read status
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun diagnosePermissions(context: android.content.Context, messageId: String) {
        viewModelScope.launch {
            try {
                val diagnostics = chatRepository.diagnosePermissions(context, messageId)
                Log.d("AdminChatVM", "Permission Diagnostics for message $messageId:")
                Log.d("AdminChatVM", diagnostics)
            } catch (e: Exception) {
                Log.e("AdminChatVM", "Diagnostic failed: ${e.message}")
            }
        }
    }
    
    fun deleteMessage(context: android.content.Context, messageId: String) {
        viewModelScope.launch {
            Log.d("AdminChatVM", "Starting delete operation for message: $messageId")
            
            // Store original messages for potential rollback
            val originalMessages = _messages.value.toList()
            
            // Optimistic UI update - remove immediately
            _messages.value = _messages.value.filterNot { it.messageId == messageId }
            
            try {
                val success = chatRepository.deleteMessage(context, messageId)
                if (!success) {
                    Log.e("AdminChatVM", "Delete operation failed for message: $messageId")
                    _error.value = "Failed to delete message from server"
                    // Rollback optimistic update
                    _messages.value = originalMessages
                    // Run diagnostics to understand why it failed
                    diagnosePermissions(context, messageId)
                } else {
                    Log.d("AdminChatVM", "Delete operation successful for message: $messageId")
                    // Add small delay to ensure Supabase operation is committed
                    kotlinx.coroutines.delay(500)
                }
            } catch (e: Exception) {
                Log.e("AdminChatVM", "Delete operation exception: ${e.message}")
                _error.value = e.message ?: "Delete failed"
                // Rollback optimistic update
                _messages.value = originalMessages
                // Run diagnostics to understand why it failed
                diagnosePermissions(context, messageId)
            }
        }
    }
    
    fun editMessage(context: android.content.Context, messageId: String, newText: String) {
        viewModelScope.launch {
            Log.d("AdminChatVM", "Starting edit operation for message: $messageId")
            Log.d("AdminChatVM", "New text: $newText")
            
            // Store original messages for potential rollback
            val originalMessages = _messages.value.toList()
            
            // Optimistic UI update - change text immediately
            _messages.value = _messages.value.map { message ->
                if (message.messageId == messageId) {
                    message.copy(text = newText )
                } else message
            }
            
            try {
                val success = chatRepository.editMessage(context, messageId, newText)
                if (!success) {
                    Log.e("AdminChatVM", "Edit operation failed for message: $messageId")
                    _error.value = "Failed to edit message on server"
                    // Rollback optimistic update
                    _messages.value = originalMessages
                    // Run diagnostics to understand why it failed
                    diagnosePermissions(context, messageId)
                } else {
                    Log.d("AdminChatVM", "Edit operation successful for message: $messageId")
                    // Add small delay to ensure Supabase operation is committed
                    kotlinx.coroutines.delay(500)
                }
            } catch (e: Exception) {
                Log.e("AdminChatVM", "Edit operation exception: ${e.message}")
                _error.value = e.message ?: "Edit failed"
                // Rollback optimistic update
                _messages.value = originalMessages
                // Run diagnostics to understand why it failed
                diagnosePermissions(context, messageId)
            }
        }
    }
}
