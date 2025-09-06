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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taskcommadmin.data.SupabaseClientProvider
import com.example.taskcommadmin.ui.navigation.Screen
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Serializable
private data class AllTasksTaskRow(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val instruction_id: String? = null,
    val created_at: String? = null
)

@Serializable
private data class AllTasksInstructionRow(
    val id: String? = null,
    val title: String? = null,
    val user_id: String? = null
)

@Serializable
private data class AllTasksUserRow(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTasksScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf<List<AllTasksTaskRow>>(emptyList()) }
    var instructions by remember { mutableStateOf<Map<String, AllTasksInstructionRow>>(emptyMap()) }
    var users by remember { mutableStateOf<Map<String, AllTasksUserRow>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val client = SupabaseClientProvider.getClient(context)
            val postgrest = client.pluginManager.getPlugin(Postgrest)
            
            // Load all tasks
            val allTasks = withContext(Dispatchers.IO) {
                postgrest["tasks"].select { }.decodeList<AllTasksTaskRow>()
            }
            
            // Get unique instruction IDs
            val instructionIds = allTasks.mapNotNull { it.instruction_id }.distinct()
            
            // Load instructions
            val instructionMap = if (instructionIds.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    val instructionRows = postgrest["instructions"].select {
                        filter {
                            or {
                                instructionIds.forEach { id ->
                                    eq("id", id)
                                }
                            }
                        }
                    }.decodeList<AllTasksInstructionRow>()
                    instructionRows.associateBy { it.id ?: "" }
                }
            } else {
                emptyMap()
            }
            
            // Get unique user IDs
            val userIds = instructionMap.values.mapNotNull { it.user_id }.distinct()
            
            // Load users
            val userMap = if (userIds.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    val userRows = postgrest["profiles"].select {
                        filter {
                            or {
                                userIds.forEach { id ->
                                    eq("id", id)
                                }
                            }
                        }
                    }.decodeList<AllTasksUserRow>()
                    userRows.associateBy { it.id ?: "" }
                }
            } else {
                emptyMap()
            }
            
            tasks = allTasks
            instructions = instructionMap
            users = userMap
            
        } catch (e: Exception) {
            error = e.message ?: "Failed to load tasks"
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "All Tasks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { /* Retry logic */ }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Tasks (${tasks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                if (tasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AssignmentLate,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No tasks found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(tasks) { task ->
                        TaskCard(
                            task = task,
                            instruction = instructions[task.instruction_id],
                            user = users[instructions[task.instruction_id]?.user_id],
                            onClick = { navController.navigate(Screen.TaskDetail.route + "/${task.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: AllTasksTaskRow,
    instruction: AllTasksInstructionRow?,
    user: AllTasksUserRow?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = task.title ?: "Untitled Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = task.status?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (task.status) {
                            "pending" -> MaterialTheme.colorScheme.tertiaryContainer
                            "in_progress" -> MaterialTheme.colorScheme.primaryContainer
                            "completed" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = task.description ?: "No description",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    instruction?.let { inst ->
                        Text(
                            text = "Instruction: ${inst.title ?: "Untitled"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    user?.let { u ->
                        Text(
                            text = "User: ${u.name ?: u.email ?: "Unknown"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                task.priority?.let { priority ->
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = priority.uppercase(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (priority.lowercase()) {
                                "high" -> MaterialTheme.colorScheme.errorContainer
                                "medium" -> MaterialTheme.colorScheme.tertiaryContainer
                                "low" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            task.created_at?.let { createdAt ->
                Text(
                    text = "Created: ${formatDate(createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
