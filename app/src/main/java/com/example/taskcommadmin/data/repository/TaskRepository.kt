package com.example.taskcommadmin.data.repository

import com.example.taskcommadmin.data.model.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await


class TaskRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    fun getTasksByUser(userId: String): Flow<List<Task>> = flow {
        try {
            val snapshot = firestore.collection("tasks")
                .whereEqualTo("adminId", userId) // adjust if tasks are assigned to userId via another field
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val tasks = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Task::class.java)?.copy(taskId = doc.id)
            }
            emit(tasks)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getTasksByInstructionIds(instructionIds: List<String>): List<Task> {
        if (instructionIds.isEmpty()) return emptyList()
        val all = mutableListOf<Task>()
        for (id in instructionIds) {
            try {
                val snapshot = firestore.collection("tasks")
                    .whereEqualTo("instructionId", id)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                val tasks = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Task::class.java)?.copy(taskId = doc.id)
                }
                all.addAll(tasks)
            } catch (_: Exception) { }
        }
        return all
    }

    fun getTasksByInstruction(instructionId: String): Flow<List<Task>> = flow {
        try {
            val snapshot = firestore.collection("tasks")
                .whereEqualTo("instructionId", instructionId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val tasks = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Task::class.java)?.copy(taskId = doc.id)
            }
            emit(tasks)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    suspend fun createTask(task: Task): String? {
        return try {
            val docRef = firestore.collection("tasks").add(task).await()
            docRef.id
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun updateTask(task: Task): Boolean {
        return try {
            firestore.collection("tasks").document(task.taskId).set(task).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateTaskStatus(taskId: String, status: String): Boolean {
        return try {
            firestore.collection("tasks").document(taskId)
                .update(
                    mapOf(
                        "status" to status,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                ).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deleteTask(taskId: String): Boolean {
        return try {
            firestore.collection("tasks").document(taskId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getTaskById(taskId: String): Task? {
        return try {
            val doc = firestore.collection("tasks").document(taskId).get().await()
            doc.toObject(Task::class.java)?.copy(taskId = doc.id)
        } catch (e: Exception) {
            null
        }
    }
}
