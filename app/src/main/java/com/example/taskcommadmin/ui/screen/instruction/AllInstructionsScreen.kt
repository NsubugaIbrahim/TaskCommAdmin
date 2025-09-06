package com.example.taskcommadmin.ui.screen.instruction

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
private data class AllInstructionsInstructionRow(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val user_id: String? = null,
    val created_at: String? = null
)

@Serializable
private data class AllInstructionsUserRow(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllInstructionsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var instructions by remember { mutableStateOf<List<AllInstructionsInstructionRow>>(emptyList()) }
    var users by remember { mutableStateOf<Map<String, AllInstructionsUserRow>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val client = SupabaseClientProvider.getClient(context)
            val postgrest = client.pluginManager.getPlugin(Postgrest)
            
            // Load all instructions
            val allInstructions = withContext(Dispatchers.IO) {
                postgrest["instructions"].select { }.decodeList<AllInstructionsInstructionRow>()
            }
            
            // Get unique user IDs
            val userIds = allInstructions.mapNotNull { it.user_id }.distinct()
            
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
                    }.decodeList<AllInstructionsUserRow>()
                    userRows.associateBy { it.id ?: "" }
                }
            } else {
                emptyMap()
            }
            
            instructions = allInstructions
            users = userMap
            
        } catch (e: Exception) {
            error = e.message ?: "Failed to load instructions"
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "All Instructions",
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
                        text = "Instructions (${instructions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                if (instructions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No instructions found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(instructions) { instruction ->
                        InstructionCard(
                            instruction = instruction,
                            user = users[instruction.user_id],
                            onClick = { navController.navigate(Screen.InstructionList.route + "/${instruction.user_id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionCard(
    instruction: AllInstructionsInstructionRow,
    user: AllInstructionsUserRow?,
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
                    text = instruction.title ?: "Untitled Instruction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = instruction.status?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (instruction.status) {
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
                text = instruction.description ?: "No description",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                user?.let { u ->
                    Text(
                        text = "User: ${u.name ?: u.email ?: "Unknown"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                instruction.created_at?.let { createdAt ->
                    Text(
                        text = "Created: ${formatDate(createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
