package com.example.taskcommadmin.data.repository

import com.example.taskcommadmin.data.model.Instruction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await


class InstructionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    fun getInstructionsByUser(userId: String): Flow<List<Instruction>> = flow {
        try {
            val snapshot = firestore.collection("instructions")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val instructions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Instruction::class.java)?.copy(instructionId = doc.id)
            }
            emit(instructions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    suspend fun getInstructionById(instructionId: String): Instruction? {
        return try {
            val doc = firestore.collection("instructions").document(instructionId).get().await()
            doc.toObject(Instruction::class.java)?.copy(instructionId = doc.id)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun updateInstructionStatus(instructionId: String, status: String): Boolean {
        return try {
            firestore.collection("instructions").document(instructionId)
                .update("status", status).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getAllInstructions(): Flow<List<Instruction>> = flow {
        try {
            val snapshot = firestore.collection("instructions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val instructions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Instruction::class.java)?.copy(instructionId = doc.id)
            }
            emit(instructions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}
