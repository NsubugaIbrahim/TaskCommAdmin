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
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionListScreen(
    navController: NavController,
    userId: String
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var instructions by remember { mutableStateOf<List<InstructionRow>>(emptyList()) }
    var userName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun fetchInstructionsForUser(targetUserId: String): List<InstructionRow> {
        return withContext(Dispatchers.IO) {
            val client = SupabaseClientProvider.getClient(navController.context)
            val postgrest = client.pluginManager.getPlugin(Postgrest)
            val uid = targetUserId.trim()
            val fkCandidates = listOf("user_id", "userId", "profile_id", "profileId", "uid")
            for (column in fkCandidates) {
                try {
                    val rows = postgrest["instructions"].select {
                        filter { eq(column, uid) }
                        order(column = "created_at", order = Order.DESCENDING)
                        limit(200)
                    }.decodeList<InstructionRow>()
                    val msg = "Tried column '" + column + "' -> fetched " + rows.size + " rows"
                    if (column == "user_id") Log.w("InstructionList", msg) else Log.d("InstructionList", msg)
                    if (rows.isNotEmpty()) return@withContext rows
                } catch (e: Exception) {
                    Log.e("InstructionList", "Fetch by column '" + column + "' failed: " + (e.message ?: "error"))
                }
            }

            // Fallback: fetch recent rows and filter client-side using any id field we can read
            return@withContext try {
                val rows = postgrest["instructions"].select {
                    order(column = "created_at", order = Order.DESCENDING)
                    limit(200)
                }.decodeList<InstructionRow>()
                val filtered = rows.filter { r ->
                    listOfNotNull(r.userId, r.userIdCamel, r.profileId, r.profileIdCamel)
                        .any { it == targetUserId }
                }
                Log.d("InstructionList", "Fallback unfiltered fetch: total=" + rows.size + ", matched=" + filtered.size)
                filtered
            } catch (e: Exception) {
                Log.e("InstructionList", "Fallback fetch failed: " + (e.message ?: "error"))
                emptyList()
            }
        }
    }

    LaunchedEffect(userId) {
        isLoading = true
        error = null
        try {
            // Fetch user name
            try {
                val client = SupabaseClientProvider.getClient(navController.context)
                val postgrest = client.pluginManager.getPlugin(Postgrest)
                val profile = withContext(Dispatchers.IO) {
                    postgrest["profiles"].select {
                        filter { eq("id", userId.trim()) }
                        limit(1)
                    }.decodeList<ProfileRow>()
                }.firstOrNull()
                userName = (profile?.name ?: profile?.email ?: "User").trim()
            } catch (_: Exception) { userName = "User" }

            // Fetch instructions
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
                title = { 
                    val count = instructions.size
                    val titleText = (if (userName.isBlank()) "User" else userName) + " • " + count.toString() + " Instructions"
                    Text(titleText)
                },
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
    // Alternative possible FK column names so client-side filter can match
    @SerialName("userId") val userIdCamel: String? = null,
    @SerialName("profile_id") val profileId: String? = null,
    @SerialName("profileId") val profileIdCamel: String? = null,
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

@Serializable
private data class ProfileRow(
    val id: String? = null,
    val email: String? = null,
    val role: String? = null,
    val name: String? = null
)


