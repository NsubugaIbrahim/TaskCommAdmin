package com.example.taskcommadmin.ui.screen.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taskcommadmin.ui.navigation.Screen
import com.example.taskcommadmin.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionTaskListScreen(
    navController: NavController,
    instructionId: String
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tasks by remember { mutableStateOf<List<TaskRow>>(emptyList()) }
    var header by remember { mutableStateOf("Tasks") }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(instructionId) {
        isLoading = true
        error = null
        try {
            val client = SupabaseClientProvider.getClient(navController.context)
            val postgrest = client.pluginManager.getPlugin(Postgrest)
            // fetch instruction + user for header
            val instr = withContext(Dispatchers.IO) {
                postgrest["instructions"].select {
                    filter { eq("id", instructionId) }
                    limit(1)
                }.decodeList<InstructionHeaderRow>()
            }.firstOrNull()
            if (instr != null) {
                val profile = withContext(Dispatchers.IO) {
                    postgrest["profiles"].select {
                        filter { eq("id", instr.userId ?: "") }
                        limit(1)
                    }.decodeList<ProfileRow>()
                }.firstOrNull()
                val userName = (profile?.name ?: profile?.email ?: "User").trim()
                val title = instr.title ?: "Instruction"
                header = "$userName • $title"
            }
            // fetch tasks for instruction
            tasks = withContext(Dispatchers.IO) {
                postgrest["tasks"].select {
                    filter { eq("instruction_id", instructionId) }
                }.decodeList<TaskRow>()
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load tasks"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(header) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddTaskDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { Text("Loading tasks...") }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { Text(error!!) }
            }
            tasks.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { Text("No tasks yet for this instruction.") }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks) { row ->
                        TaskRowCard(row) {
                            val taskId = row.id ?: ""
                            if (taskId.isNotBlank()) {
                                navController.navigate(Screen.Chat.route + "/" + taskId)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onTaskCreated = { title, description, priority ->
                scope.launch {
                    try {
                        val client = SupabaseClientProvider.getClient(navController.context)
                        val postgrest = client.pluginManager.getPlugin(Postgrest)
                        withContext(Dispatchers.IO) {
                            postgrest["tasks"].insert(NewTaskRow(
                                instructionId = instructionId,
                                title = title,
                                description = description,
                                priority = priority,
                                status = "pending"
                            ))
                        }
                        // refresh
                        isLoading = true
                        tasks = withContext(Dispatchers.IO) {
                            postgrest["tasks"].select {
                                filter { eq("instruction_id", instructionId) }
                            }.decodeList<TaskRow>()
                        }
                        showAddTaskDialog = false
                    } catch (e: Exception) {
                        error = e.message ?: "Failed to create task"
                    } finally {
                        isLoading = false
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRowCard(row: TaskRow, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.title ?: "<untitled>",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!row.description.isNullOrBlank()) {
                        Text(
                            text = row.description!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LocalStatusChip(row.status ?: "pending")
                    Spacer(modifier = Modifier.width(8.dp))
                    LocalPriorityChip(row.priority ?: "medium")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = row.createdAtFormatted(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocalStatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "pending" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        "in_progress" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "completed" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = backgroundColor, shape = MaterialTheme.shapes.small) {
        Text(
            text = status.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Serializable
private data class TaskRow(
    val id: String? = null,
    @SerialName("instruction_id") val instructionId: String? = null,
    @SerialName("admin_id") val adminId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun createdAtFormatted(): String {
        return try {
            val parser = java.time.OffsetDateTime.parse(createdAt)
            val date = Date.from(parser.toInstant())
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
        } catch (_: Exception) { "" }
    }
}

@Serializable
private data class NewTaskRow(
    @SerialName("instruction_id") val instructionId: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String
)

@Composable
private fun LocalPriorityChip(priority: String) {
    val (backgroundColor, textColor) = when (priority) {
        "low" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        "medium" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "high" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = backgroundColor, shape = MaterialTheme.shapes.small) {
        Text(
            text = priority.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Serializable
private data class InstructionHeaderRow(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val title: String? = null
)

@Serializable
private data class ProfileRow(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null
)


