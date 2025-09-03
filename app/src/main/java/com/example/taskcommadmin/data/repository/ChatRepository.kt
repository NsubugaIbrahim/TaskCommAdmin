package com.example.taskcommadmin.data.repository

import com.example.taskcommadmin.data.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.io.InputStream


class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    fun getMessagesByTask(taskId: String): Flow<List<ChatMessage>> = flow {
        try {
            val snapshot = firestore.collection("chat_messages")
                .whereEqualTo("taskId", taskId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val messages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(messageId = doc.id)
            }
            emit(messages)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    suspend fun sendMessage(message: ChatMessage): String? {
        return try {
            val docRef = firestore.collection("chat_messages").add(message).await()
            docRef.id
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun uploadFile(
        taskId: String,
        fileName: String,
        inputStream: InputStream,
        fileType: String
    ): String? {
        return try {
            val storageRef = storage.reference
                .child("chat_files")
                .child(taskId)
                .child(fileName)
            
            val uploadTask = storageRef.putStream(inputStream).await()
            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun markMessageAsRead(messageId: String): Boolean {
        return try {
            firestore.collection("chat_messages").document(messageId)
                .update("isRead", true).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getUnreadMessageCount(taskId: String): Int {
        return try {
            val snapshot = firestore.collection("chat_messages")
                .whereEqualTo("taskId", taskId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }
}
