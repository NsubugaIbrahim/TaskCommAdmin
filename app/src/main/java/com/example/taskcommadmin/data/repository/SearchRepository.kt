package com.example.taskcommadmin.data.repository

import android.content.Context
import com.example.taskcommadmin.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class UserSearchResult(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
    val created_at: String? = null
)

@Serializable
data class TaskSearchResult(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val instruction_id: String? = null,
    val created_at: String? = null
)

@Serializable
data class InstructionSearchResult(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val user_id: String? = null,
    val created_at: String? = null
)

@Serializable
data class ChatMessageSearchResult(
    val id: String? = null,
    val text: String? = null,
    val sender_id: String? = null,
    val sender_role: String? = null,
    val task_id: String? = null,
    val created_at: String? = null
)

data class SearchResults(
    val users: List<UserSearchResult>,
    val tasks: List<TaskSearchResult>,
    val instructions: List<InstructionSearchResult>,
    val messages: List<ChatMessageSearchResult>
)

class SearchRepository {
    
    suspend fun searchAll(query: String, context: Context): SearchResults = withContext(Dispatchers.IO) {
        val client = SupabaseClientProvider.getClient(context)
        val postgrest = client.pluginManager.getPlugin(Postgrest)
        
        try {
            // Search users
            val users = postgrest["profiles"].select {
                filter {
                    or {
                        ilike("name", "%$query%")
                        ilike("email", "%$query%")
                    }
                }
                limit(20)
            }.decodeList<UserSearchResult>()
            
            // Search tasks
            val tasks = postgrest["tasks"].select {
                filter {
                    or {
                        ilike("title", "%$query%")
                        ilike("description", "%$query%")
                    }
                }
                limit(20)
            }.decodeList<TaskSearchResult>()
            
            // Search instructions
            val instructions = postgrest["instructions"].select {
                filter {
                    or {
                        ilike("title", "%$query%")
                        ilike("description", "%$query%")
                    }
                }
                limit(20)
            }.decodeList<InstructionSearchResult>()
            
            // Search chat messages
            val messages = postgrest["chat_messages"].select {
                filter {
                    ilike("text", "%$query%")
                }
                limit(20)
            }.decodeList<ChatMessageSearchResult>()
            
            SearchResults(
                users = users,
                tasks = tasks,
                instructions = instructions,
                messages = messages
            )
        } catch (e: Exception) {
            println("Search error: ${e.message}")
            SearchResults(emptyList(), emptyList(), emptyList(), emptyList())
        }
    }
    
    suspend fun searchUsers(query: String, context: Context): List<UserSearchResult> = withContext(Dispatchers.IO) {
        val client = SupabaseClientProvider.getClient(context)
        val postgrest = client.pluginManager.getPlugin(Postgrest)
        
        try {
            postgrest["profiles"].select {
                filter {
                    or {
                        ilike("name", "%$query%")
                        ilike("email", "%$query%")
                    }
                }
                limit(50)
            }.decodeList<UserSearchResult>()
        } catch (e: Exception) {
            println("User search error: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun searchTasks(query: String, context: Context): List<TaskSearchResult> = withContext(Dispatchers.IO) {
        val client = SupabaseClientProvider.getClient(context)
        val postgrest = client.pluginManager.getPlugin(Postgrest)
        
        try {
            postgrest["tasks"].select {
                filter {
                    or {
                        ilike("title", "%$query%")
                        ilike("description", "%$query%")
                    }
                }
                limit(50)
            }.decodeList<TaskSearchResult>()
        } catch (e: Exception) {
            println("Task search error: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun searchInstructions(query: String, context: Context): List<InstructionSearchResult> = withContext(Dispatchers.IO) {
        val client = SupabaseClientProvider.getClient(context)
        val postgrest = client.pluginManager.getPlugin(Postgrest)
        
        try {
            postgrest["instructions"].select {
                filter {
                    or {
                        ilike("title", "%$query%")
                        ilike("description", "%$query%")
                    }
                }
                limit(50)
            }.decodeList<InstructionSearchResult>()
        } catch (e: Exception) {
            println("Instruction search error: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun searchMessages(query: String, context: Context): List<ChatMessageSearchResult> = withContext(Dispatchers.IO) {
        val client = SupabaseClientProvider.getClient(context)
        val postgrest = client.pluginManager.getPlugin(Postgrest)
        
        try {
            postgrest["chat_messages"].select {
                filter {
                    ilike("text", "%$query%")
                }
                limit(50)
            }.decodeList<ChatMessageSearchResult>()
        } catch (e: Exception) {
            println("Message search error: ${e.message}")
            emptyList()
        }
    }
}
