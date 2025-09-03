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

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var currentTaskId: String? = null
    
    fun loadMessages(taskId: String) {
        currentTaskId = taskId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                chatRepository.getMessagesByTask(taskId).collect { messageList ->
                    _messages.value = messageList
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load messages"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendTextMessage(
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
                
                val messageId = chatRepository.sendMessage(message)
                if (messageId == null) {
                    _error.value = "Failed to send message"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Send message failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendFileMessage(
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
                    
                    val messageId = chatRepository.sendMessage(message)
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
    
    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.markMessageAsRead(messageId)
            } catch (e: Exception) {
                // Silent fail for read status
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
