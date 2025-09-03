package com.example.taskcommadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskcommadmin.data.model.User
import com.example.taskcommadmin.data.repository.UserRepository
import com.example.taskcommadmin.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import com.google.firebase.Timestamp
import java.time.OffsetDateTime
import java.util.Date

class UserManagementViewModel : ViewModel() {
    private val userRepository = UserRepository()
    
    // Remember the last used application context so we can refresh after updates/deletes
    private var lastAppContext: Context? = null

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Call from UI with a Context (needed for Supabase client)
    fun loadUsers(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                lastAppContext = context.applicationContext
                val client = SupabaseClientProvider.getClient(context)
                val postgrest = client.pluginManager.getPlugin(Postgrest)
                val rows = postgrest["profiles"].select {
                    filter { eq("role", "user") }
                    order(column = "created_at", order = Order.DESCENDING)
                }.decodeList<ProfileRow>()
                val mapped = rows.map { row ->
                    User(
                        userId = row.id ?: "",
                        name = row.name ?: "",
                        address = "",
                        businessField = "",
                        email = row.email ?: "",
                        createdAt = parseSupabaseTimestamp(row.createdAt),
                        isActive = true
                    )
                }
                _users.value = mapped
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load users"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchUsers(context: Context, query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                lastAppContext = context.applicationContext
                val client = SupabaseClientProvider.getClient(context)
                val postgrest = client.pluginManager.getPlugin(Postgrest)
                val rows = postgrest["profiles"].select {
                    filter { eq("role", "user") }
                    // Simple server-side filter; refine as needed
                }.decodeList<ProfileRow>()
                val filtered = rows.filter { r ->
                    r.name?.contains(query, ignoreCase = true) == true ||
                    r.email?.contains(query, ignoreCase = true) == true
                }
                _users.value = filtered.map { r ->
                    User(
                        userId = r.id ?: "",
                        name = r.name ?: "",
                        address = "",
                        businessField = "",
                        email = r.email ?: "",
                        createdAt = parseSupabaseTimestamp(r.createdAt),
                        isActive = true
                    )
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Search failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateUser(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = userRepository.updateUser(user)
                if (success) {
                    lastAppContext?.let { loadUsers(it) } // Reload the list if possible
                } else {
                    _error.value = "Failed to update user"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Update failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = userRepository.deleteUser(userId)
                if (success) {
                    lastAppContext?.let { loadUsers(it) } // Reload the list if possible
                } else {
                    _error.value = "Failed to delete user"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Delete failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }

    @Serializable
    private data class ProfileRow(
        val id: String? = null,
        val email: String? = null,
        val role: String? = null,
        val name: String? = null,
        @SerialName("created_at") val createdAt: String? = null
    )

    private fun parseSupabaseTimestamp(value: String?): Timestamp {
        return try {
            val odt = OffsetDateTime.parse(value)
            Timestamp(Date.from(odt.toInstant()))
        } catch (_: Exception) {
            Timestamp.now()
        }
    }
}
