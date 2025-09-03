package com.example.taskcommadmin.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope

import androidx.navigation.NavController
import com.example.taskcommadmin.ui.navigation.Screen
import com.example.taskcommadmin.ui.viewmodel.AuthViewModel
import com.example.taskcommadmin.data.repository.UserRepository
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = AuthViewModel()
) {
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    
    var testResult by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var nonAdminUsers by remember { mutableStateOf<List<ProfileRow>>(emptyList()) }
    var instructions by remember { mutableStateOf<List<InstructionRow>>(emptyList()) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            val client = SupabaseClientProvider.getClient(ctx)
            val postgrest = client.pluginManager.getPlugin(Postgrest)
            // Non-admin users
            val users = withContext(Dispatchers.IO) {
                postgrest["profiles"].select {
                    filter { eq("role", "user") }
                }.decodeList<ProfileRow>()
            }
            nonAdminUsers = users
            // Instructions
            val instr = withContext(Dispatchers.IO) {
                postgrest["instructions"].select {
                    // order by latest if supported in DSL; otherwise rely on default
                }.decodeList<InstructionRow>()
            }
            instructions = instr
        } catch (e: Exception) {
            error = e.message ?: "Failed to load data from Supabase"
        } finally {
            loading = false
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TaskComm Admin Dashboard") },
                actions = {
                    IconButton(onClick = { authViewModel.signOut(ctx) }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to TaskComm Admin",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
            
            // Firebase Test Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            testResult = "Testing Firebase connection..."
                            val success = userRepository.testFirebaseConnection()
                            testResult = if (success) "Firebase connection successful!" else "Firebase connection failed!"
                        }
                    }
                ) {
                    Text("Test Firebase")
                }
                
                Button(
                    onClick = {
                        scope.launch {
                            testResult = "Testing users collection..."
                            val success = userRepository.testUsersCollection()
                            testResult = if (success) "Users collection accessible!" else "Users collection access failed!"
                        }
                    }
                ) {
                    Text("Test Users Collection")
                }
            }
            
            if (testResult != null) {
                Text(
                    text = testResult!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (testResult!!.contains("successful")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // User Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(Screen.UserList.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "User Management",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Manage users, view profiles, and handle accounts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Instructions Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* Navigate to instructions overview */ }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Instructions Overview",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "View all instructions from users",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Supabase: Recent Instructions
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Recent Instructions (Supabase)", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = {
                            scope.launch {
                                loading = true; error = null
                                try {
                                    val client = SupabaseClientProvider.getClient(ctx)
                                    val postgrest = client.pluginManager.getPlugin(Postgrest)
                                    instructions = withContext(Dispatchers.IO) {
                                        postgrest["instructions"].select { }.decodeList()
                                    }
                                } catch (e: Exception) {
                                    error = e.message
                                } finally { loading = false }
                            }
                        }) { Text("Refresh") }
                    }
                    if (instructions.isEmpty()) {
                        Text(text = "No instructions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        instructions.take(10).forEach { ins ->
                            val uid = ins.user_id ?: ins.userId ?: ""
                            Text(text = (ins.title ?: "<no title>") + "  —  user=" + uid + "  —  status=" + (ins.status ?: ""))
                        }
                    }
                }
            }
            
            // Task Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* Navigate to task management */ }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Task Management",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Create and manage tasks for users",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Communication Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* Navigate to communication center */ }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Communication Center",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Chat with users and manage communications",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
