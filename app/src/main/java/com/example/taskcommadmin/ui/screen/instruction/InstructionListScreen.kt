package com.example.taskcommadmin.ui.screen.instruction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionListScreen(
    navController: NavController,
    userId: String
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var instructions by remember { mutableStateOf<List<InstructionRow>>(emptyList()) }
    val scope = rememberCoroutineScope()

    suspend fun fetchInstructionsForUser(targetUserId: String): List<InstructionRow> {
        return withContext(Dispatchers.IO) {
            val client = SupabaseClientProvider.getClient(navController.context)
            val postgrest = client.pluginManager.getPlugin(Postgrest)
            postgrest["instructions"].select {
                filter { eq("user_id", targetUserId) }
                order(column = "created_at", order = Order.DESCENDING)
                // optional cap to avoid huge payloads
                limit(200)
            }.decodeList<InstructionRow>()
        }
    }

    LaunchedEffect(userId) {
        isLoading = true
        error = null
        try {
            val rows = fetchInstructionsForUser(userId)
            instructions = rows
        } catch (e: Exception) {
            error = e.message ?: "Failed to load instructions"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instructions") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                ) { Text("Loading instructions...") }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            // retry
                            isLoading = true
                            error = null
                            instructions = emptyList()
                            // trigger LaunchedEffect by assigning same userId won't help; re-fetch inline
                            // so do the same fetch logic here
                            // Inlining to avoid duplicating state holders
                            // Use snapshot of userId
                            val uid = userId
                            scope.launch {
                                try {
                                    val rows = fetchInstructionsForUser(uid)
                                    instructions = rows
                                    error = null
                                } catch (e: Exception) {
                                    error = e.message ?: "Failed to load instructions"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }) { Text("Retry") }
                    }
                }
            }
            instructions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { Text("No instructions found for this user.") }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(instructions) { item ->
                        InstructionCard(
                            instruction = item,
                            onOpenTasks = { navController.navigate(Screen.TaskList.route + "/${'$'}{userId}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionCard(
    instruction: InstructionRow,
    onOpenTasks: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                        text = instruction.title ?: "<untitled>",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!instruction.description.isNullOrBlank()) {
                        Text(
                            text = instruction.description!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                StatusChip(instruction.status ?: "pending")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = instruction.createdAtFormatted(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpenTasks) { Text("View Tasks") }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
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
            text = status.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Serializable
private data class InstructionRow(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun createdAtFormatted(): String {
        return try {
            // created_at is ISO-8601 string from Supabase
            val parser = java.time.OffsetDateTime.parse(createdAt)
            val date = Date.from(parser.toInstant())
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
        } catch (_: Exception) {
            ""
        }
    }
}


