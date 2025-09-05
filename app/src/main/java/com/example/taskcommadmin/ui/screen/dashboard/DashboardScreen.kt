package com.example.taskcommadmin.ui.screen.dashboard

import androidx.compose.foundation.layout.*
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
import com.example.taskcommadmin.ui.navigation.Screen
import com.example.taskcommadmin.ui.viewmodel.AuthViewModel
import com.example.taskcommadmin.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
 

@Serializable
private data class ProfileRow(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null
)

@Serializable
private data class InstructionRow(
    val id: String? = null,
    val user_id: String? = null,
    val userId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null
)

@Serializable
private data class TaskRow(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = AuthViewModel()
) {
    val context = LocalContext.current
    var adminName by remember { mutableStateOf("Admin") }
    var stats by remember { mutableStateOf(DashboardStats()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val client = SupabaseClientProvider.getClient(context)
            val postgrest = client.pluginManager.getPlugin(Postgrest)
            val auth = client.pluginManager.getPlugin(Auth)
            
            // Get admin profile
            val currentUser = auth.currentSessionOrNull()?.user
            if (currentUser != null) {
                val profile = withContext(Dispatchers.IO) {
                    postgrest["profiles"].select {
                        filter { eq("id", currentUser.id) }
                        limit(1)
                    }.decodeList<ProfileRow>()
                }.firstOrNull()
                
                adminName = profile?.name ?: profile?.email ?: "Admin"
                
                // Get dashboard statistics
                val userCount = withContext(Dispatchers.IO) {
                    postgrest["profiles"].select {
                        filter { eq("role", "user") }
                    }.decodeList<ProfileRow>().size.toLong()
                }
                
                val instructionCount = withContext(Dispatchers.IO) {
                    postgrest["instructions"].select { }.decodeList<InstructionRow>().size.toLong()
                }
                
                val taskCount = withContext(Dispatchers.IO) {
                    postgrest["tasks"].select { }.decodeList<TaskRow>().size.toLong()
                }
                
                stats = DashboardStats(
                    userCount = userCount,
                    instructionCount = instructionCount,
                    taskCount = taskCount
                )
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load dashboard data"
        } finally {
            loading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "TaskComm Admin",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Welcome, $adminName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = { authViewModel.signOut(context) }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (loading) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Statistics Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Users",
                        value = stats.userCount.toString(),
                        icon = Icons.Default.People,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Instructions",
                        value = stats.instructionCount.toString(),
                        icon = Icons.Default.Description,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Tasks",
                        value = stats.taskCount.toString(),
                        icon = Icons.Default.Assignment,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Quick Actions
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // User Management Card
                DashboardCard(
                    title = "User Management",
                    description = "Manage users, view profiles, and handle accounts",
                    icon = Icons.Default.People,
                    onClick = { navController.navigate(Screen.UserList.route) }
                )
                
                // Task Management Card
                DashboardCard(
                    title = "Task Management",
                    description = "Create and manage tasks for users",
                    icon = Icons.Default.Assignment,
                    onClick = { /* Navigate to task management */ }
                )
                
                // Instructions Overview Card
                DashboardCard(
                    title = "Instructions Overview",
                    description = "View all instructions from users",
                    icon = Icons.Default.Description,
                    onClick = { /* Navigate to instructions overview */ }
                )
                
                // Communication Center Card
                DashboardCard(
                    title = "Communication Center",
                    description = "Chat with users and manage communications",
                    icon = Icons.Default.Chat,
                    onClick = { /* Navigate to communication center */ }
                )
                
                // Search Card
                DashboardCard(
                    title = "Global Search",
                    description = "Search across users, tasks, instructions, and messages",
                    icon = Icons.Default.Search,
                    onClick = { navController.navigate(Screen.Search.route) }
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class DashboardStats(
    val userCount: Long = 0,
    val instructionCount: Long = 0,
    val taskCount: Long = 0
)
