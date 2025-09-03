package com.example.taskcommadmin.ui.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskcommadmin.data.model.Task
import com.example.taskcommadmin.data.repository.TaskRepository
import com.example.taskcommadmin.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskManagementViewModel : ViewModel() {
    private val taskRepository = TaskRepository()
    
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var currentInstructionId: String? = null
    private var currentUserId: String? = null
    
    fun loadTasks(instructionId: String) {
        currentInstructionId = instructionId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                taskRepository.getTasksByInstruction(instructionId).collect { taskList ->
                    _tasks.value = taskList
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load tasks"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTasksByUser(context: android.content.Context, userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // 1) Get instruction IDs for user from Supabase
                val client = SupabaseClientProvider.getClient(context)
                val postgrest = client.pluginManager.getPlugin(Postgrest)
                val rows = withContext(Dispatchers.IO) {
                    postgrest["instructions"].select {
                        filter { eq("user_id", userId) }
                    }.decodeList<InstructionRow>()
                }
                val instructionIds = rows.mapNotNull { it.id }
                // 2) Fetch tasks from Supabase tasks table for each instruction id
                val rowsAll = withContext(Dispatchers.IO) {
                    val collected = mutableListOf<TaskRow>()
                    for (iid in instructionIds) {
                        val rows = try {
                            postgrest["tasks"].select {
                                filter { eq("instruction_id", iid) }
                            }.decodeList<TaskRow>()
                        } catch (_: Exception) { emptyList() }
                        collected.addAll(rows)
                    }
                    collected
                }
                _tasks.value = rowsAll.map { it.toModel() }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load tasks"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createTask(context: android.content.Context, task: Task) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val client = SupabaseClientProvider.getClient(context)
                val postgrest = client.pluginManager.getPlugin(Postgrest)
                postgrest["tasks"].insert(TaskRow.fromModel(task))
                currentInstructionId?.let { loadTasks(it) }
                currentUserId?.let { loadTasksByUser(context, it) }
            } catch (e: Exception) {
                _error.value = e.message ?: "Create task failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateTask(context: android.content.Context, task: Task) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val client = SupabaseClientProvider.getClient(context)
                val postgrest = client.pluginManager.getPlugin(Postgrest)
                postgrest["tasks"].upsert(TaskRow.fromModel(task))
                currentInstructionId?.let { loadTasks(it) }
                currentUserId?.let { loadTasksByUser(context, it) }
            } catch (e: Exception) {
                _error.value = e.message ?: "Update task failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateTaskStatus(taskId: String, status: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Requires context to post to Supabase; keep old path for now
                _error.value = "Use updateTask(context, task.copy(status = ...)) instead"
            } catch (e: Exception) {
                _error.value = e.message ?: "Update status failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteTask(context: android.content.Context, taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val client = SupabaseClientProvider.getClient(context)
                val postgrest = client.pluginManager.getPlugin(Postgrest)
                postgrest["tasks"].delete {
                    filter { eq("id", taskId) }
                }
                currentInstructionId?.let { loadTasks(it) }
                currentUserId?.let { loadTasksByUser(context, it) }
            } catch (e: Exception) {
                _error.value = e.message ?: "Delete task failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
private data class TaskRow(
    val id: String? = null,
    @SerialName("instruction_id") val instructionId: String? = null,
    @SerialName("admin_id") val adminId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null
) {
    fun toModel(): Task = Task(
        taskId = id ?: "",
        instructionId = instructionId ?: "",
        adminId = adminId ?: "",
        title = title ?: "",
        description = description ?: "",
        status = status ?: "pending",
        priority = priority ?: "medium"
    )
    companion object {
        fun fromModel(t: Task): TaskRow = TaskRow(
            id = if (t.taskId.isBlank()) null else t.taskId,
            instructionId = t.instructionId,
            adminId = t.adminId,
            title = t.title,
            description = t.description,
            status = t.status,
            priority = t.priority
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
private data class InstructionRow(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null
)
