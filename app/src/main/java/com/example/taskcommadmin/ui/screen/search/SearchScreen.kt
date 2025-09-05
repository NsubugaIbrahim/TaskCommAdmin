package com.example.taskcommadmin.ui.screen.search

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taskcommadmin.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Serializable
private data class UserSearchResult(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null
)

@Serializable
private data class TaskSearchResult(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val created_at: String? = null
)

@Serializable
private data class InstructionSearchResult(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val user_id: String? = null,
    val created_at: String? = null
)

@Serializable
private data class ChatMessageSearchResult(
    val id: String? = null,
    val text: String? = null,
    val sender_role: String? = null,
    val task_id: String? = null,
    val created_at: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<SearchResults?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf("All", "Users", "Tasks", "Instructions", "Messages")
    
    val performSearch = { query: String ->
        if (query.isNotBlank()) {
            isLoading = true
            // Perform search in background
            // This would be implemented with actual search logic
            searchResults = SearchResults(
                users = emptyList(),
                tasks = emptyList(),
                instructions = emptyList(),
                messages = emptyList()
            )
            isLoading = false
        }
    }
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            performSearch(searchQuery)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search users, tasks, instructions, messages...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            // Search Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Search Results
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (searchQuery.length < 2) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Start typing to search",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Search across users, tasks, instructions, and messages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // All
                            searchResults?.let { results ->
                                if (results.users.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Users (${results.users.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(results.users) { user ->
                                        UserSearchItem(user = user, navController = navController)
                                    }
                                }
                                
                                if (results.tasks.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Tasks (${results.tasks.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(results.tasks) { task ->
                                        TaskSearchItem(task = task, navController = navController)
                                    }
                                }
                                
                                if (results.instructions.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Instructions (${results.instructions.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(results.instructions) { instruction ->
                                        InstructionSearchItem(instruction = instruction, navController = navController)
                                    }
                                }
                                
                                if (results.messages.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Messages (${results.messages.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(results.messages) { message ->
                                        MessageSearchItem(message = message, navController = navController)
                                    }
                                }
                            }
                        }
                        1 -> { // Users
                            searchResults?.users?.let { users ->
                                items(users) { user ->
                                    UserSearchItem(user = user, navController = navController)
                                }
                            }
                        }
                        2 -> { // Tasks
                            searchResults?.tasks?.let { tasks ->
                                items(tasks) { task ->
                                    TaskSearchItem(task = task, navController = navController)
                                }
                            }
                        }
                        3 -> { // Instructions
                            searchResults?.instructions?.let { instructions ->
                                items(instructions) { instruction ->
                                    InstructionSearchItem(instruction = instruction, navController = navController)
                                }
                            }
                        }
                        4 -> { // Messages
                            searchResults?.messages?.let { messages ->
                                items(messages) { message ->
                                    MessageSearchItem(message = message, navController = navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSearchItem(
    user: UserSearchResult,
    navController: NavController
) {
    Card(
        onClick = { navController.navigate("user_detail/${user.id}") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = user.email ?: "No email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = { },
                label = { Text(user.role?.uppercase() ?: "USER") }
            )
        }
    }
}

@Composable
private fun TaskSearchItem(
    task: TaskSearchResult,
    navController: NavController
) {
    Card(
        onClick = { navController.navigate("task_detail/${task.id}") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title ?: "Untitled Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = task.description ?: "No description",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            AssistChip(
                onClick = { },
                label = { Text(task.status?.uppercase() ?: "UNKNOWN") }
            )
        }
    }
}

@Composable
private fun InstructionSearchItem(
    instruction: InstructionSearchResult,
    navController: NavController
) {
    Card(
        onClick = { navController.navigate("instruction_list/${instruction.user_id}") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = instruction.title ?: "Untitled Instruction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = instruction.description ?: "No description",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            AssistChip(
                onClick = { },
                label = { Text(instruction.status?.uppercase() ?: "UNKNOWN") }
            )
        }
    }
}

@Composable
private fun MessageSearchItem(
    message: ChatMessageSearchResult,
    navController: NavController
) {
    Card(
        onClick = { navController.navigate("chat/${message.task_id}") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.text ?: "No message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                Text(
                    text = "From: ${message.sender_role?.uppercase() ?: "UNKNOWN"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = { },
                label = { Text("CHAT") }
            )
        }
    }
}

private data class SearchResults(
    val users: List<UserSearchResult>,
    val tasks: List<TaskSearchResult>,
    val instructions: List<InstructionSearchResult>,
    val messages: List<ChatMessageSearchResult>
)
