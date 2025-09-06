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
import com.example.taskcommadmin.data.repository.SearchResults
import com.example.taskcommadmin.data.repository.UserSearchResult
import com.example.taskcommadmin.data.repository.TaskSearchResult
import com.example.taskcommadmin.data.repository.InstructionSearchResult
import com.example.taskcommadmin.data.repository.ChatMessageSearchResult
import com.example.taskcommadmin.ui.viewmodel.SearchViewModel
import com.example.taskcommadmin.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    searchViewModel: SearchViewModel = SearchViewModel()
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    val error by searchViewModel.error.collectAsState()
    
    val tabs = listOf("All", "Users", "Tasks", "Instructions", "Messages")
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            when (selectedTab) {
                0 -> searchViewModel.search(searchQuery, context)
                1 -> searchViewModel.searchUsers(searchQuery, context)
                2 -> searchViewModel.searchTasks(searchQuery, context)
                3 -> searchViewModel.searchInstructions(searchQuery, context)
                4 -> searchViewModel.searchMessages(searchQuery, context)
            }
        } else {
            searchViewModel.clearResults()
        }
    }
    
    LaunchedEffect(selectedTab) {
        if (searchQuery.length >= 2) {
            when (selectedTab) {
                0 -> searchViewModel.search(searchQuery, context)
                1 -> searchViewModel.searchUsers(searchQuery, context)
                2 -> searchViewModel.searchTasks(searchQuery, context)
                3 -> searchViewModel.searchInstructions(searchQuery, context)
                4 -> searchViewModel.searchMessages(searchQuery, context)
            }
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
            
            // Error Display
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
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
            } else if (searchResults != null) {
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
                                
                                if (results.users.isEmpty() && results.tasks.isEmpty() && 
                                    results.instructions.isEmpty() && results.messages.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.SearchOff,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(64.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "No results found",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Try a different search term",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Users
                            searchResults?.users?.let { users ->
                                if (users.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.PersonOff,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(64.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "No users found",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(users) { user ->
                                        UserSearchItem(user = user, navController = navController)
                                    }
                                }
                            }
                        }
                        2 -> { // Tasks
                            searchResults?.tasks?.let { tasks ->
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
                                        TaskSearchItem(task = task, navController = navController)
                                    }
                                }
                            }
                        }
                        3 -> { // Instructions
                            searchResults?.instructions?.let { instructions ->
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
                                        InstructionSearchItem(instruction = instruction, navController = navController)
                                    }
                                }
                            }
                        }
                        4 -> { // Messages
                            searchResults?.messages?.let { messages ->
                                if (messages.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.ChatBubbleOutline,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(64.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "No messages found",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
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
}

@Composable
private fun UserSearchItem(
    user: UserSearchResult,
    navController: NavController
) {
    Card(
        onClick = { navController.navigate(Screen.UserDetail.route + "/${user.id}") },
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
        onClick = { navController.navigate(Screen.TaskDetail.route + "/${task.id}") },
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
        onClick = { navController.navigate(Screen.InstructionList.route + "/${instruction.user_id}") },
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
        onClick = { navController.navigate(Screen.Chat.route + "/${message.task_id}") },
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

