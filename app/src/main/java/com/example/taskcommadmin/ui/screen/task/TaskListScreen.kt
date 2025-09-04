package com.example.taskcommadmin.ui.screen.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.navigation.NavController
import com.example.taskcommadmin.data.model.Task
import com.example.taskcommadmin.ui.navigation.Screen
import com.example.taskcommadmin.ui.viewmodel.TaskManagementViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    instructionId: String,
    viewModel: TaskManagementViewModel = TaskManagementViewModel()
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val tasks by viewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var instructionsForUser by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedInstructionId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(instructionId) {
        // instructionId now holds the userId per NavGraph
        viewModel.loadTasksByUser(ctx, instructionId)
        // Preload instruction list from Supabase for this user to create tasks under
        try {
            val client = com.example.taskcommadmin.data.SupabaseClientProvider.getClient(navController.context)
            val postgrest = client.pluginManager.getPlugin(io.github.jan.supabase.postgrest.Postgrest)
            val rows = postgrest["instructions"].select {
                filter { eq("user_id", instructionId) }
            }.decodeList<InstructionRow>()
            instructionsForUser = rows.map { (it.id ?: "") to (it.title ?: "<untitled>") }
            selectedInstructionId = instructionsForUser.firstOrNull()?.first
        } catch (_: Exception) { }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading tasks...")
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadTasksByUser(ctx, instructionId) }) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks) { task ->
                        TaskCard(
                            task = task,
                            onTaskClick = { navController.navigate(Screen.Chat.route + "/${task.taskId}") },
                            onStatusChange = { newStatus ->
                                viewModel.updateTask(ctx, task.copy(status = newStatus))
                            }
                        )
                    }
                }
            }
        }
        
        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onTaskCreated = { title, description, priority ->
                    val targetInstructionId = selectedInstructionId
                        ?: instructionsForUser.firstOrNull()?.first
                        ?: instructionId
                    val newTask = Task(
                        instructionId = targetInstructionId,
                        title = title,
                        description = description,
                        priority = priority
                    )
                    viewModel.createTask(ctx, newTask)
                    showAddTaskDialog = false
                }
            , extraContent = {
                // Extra content to choose target instruction
                if (instructionsForUser.isNotEmpty()) {
                    Text("Attach to instruction", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    instructionsForUser.forEach { (id, title) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedInstructionId == id,
                                onClick = { selectedInstructionId = id }
                            )
                            Text(title)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            })
        }
    }
}

@kotlinx.serialization.Serializable
private data class InstructionRow(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    @kotlinx.serialization.SerialName("user_id") val userId: String? = null,
    val status: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onTaskClick: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onTaskClick
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
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = task.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    PriorityChip(priority = task.priority)
                }
                
                Text(
                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(task.createdAt.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "pending" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        "in_progress" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "completed" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.replace("_", " ").capitalize(),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PriorityChip(priority: String) {
    val (backgroundColor, textColor) = when (priority) {
        "low" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        "medium" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "high" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = priority.capitalize(),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onTaskCreated: (String, String, String) -> Unit,
    extraContent: @Composable () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Priority", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("low", "medium", "high").forEach { priorityOption ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = priority == priorityOption,
                                onClick = { priority = priorityOption }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(priorityOption.capitalize())
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                extraContent()
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onTaskCreated(title, description, priority)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
