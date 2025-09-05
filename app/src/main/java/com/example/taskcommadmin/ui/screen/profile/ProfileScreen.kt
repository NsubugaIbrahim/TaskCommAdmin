package com.example.taskcommadmin.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val role: String? = null,
    val created_at: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = AuthViewModel()
) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf<ProfileRow?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            val client = SupabaseClientProvider.getClient(context)
            val postgrest = client.pluginManager.getPlugin(Postgrest)
            val auth = client.pluginManager.getPlugin(Auth)
            
            val currentUser = auth.currentSessionOrNull()?.user
            if (currentUser != null) {
                val profileData = withContext(Dispatchers.IO) {
                    postgrest["profiles"].select {
                        filter { eq("id", currentUser.id) }
                        limit(1)
                    }.decodeList<ProfileRow>()
                }.firstOrNull()
                
                profile = profileData
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load profile"
        } finally {
            loading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        authViewModel.signOut(context)
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }) {
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
                    Button(onClick = { navController.navigateUp() }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = profile?.name ?: "Admin User",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = profile?.email ?: "admin@taskcomm.com",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = profile?.role?.uppercase() ?: "ADMIN",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                labelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                
                // Profile Information
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Profile Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        ProfileInfoRow(
                            label = "User ID",
                            value = profile?.id ?: "N/A"
                        )
                        
                        ProfileInfoRow(
                            label = "Email",
                            value = profile?.email ?: "N/A"
                        )
                        
                        ProfileInfoRow(
                            label = "Display Name",
                            value = profile?.name ?: "N/A"
                        )
                        
                        ProfileInfoRow(
                            label = "Role",
                            value = profile?.role?.uppercase() ?: "ADMIN"
                        )
                        
                        ProfileInfoRow(
                            label = "Account Created",
                            value = profile?.created_at?.let { 
                                try {
                                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                                        .parse(it)?.let { date ->
                                            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(date)
                                        } ?: "N/A"
                                } catch (e: Exception) {
                                    "N/A"
                                }
                            } ?: "N/A"
                        )
                    }
                }
                
                // Admin Statistics
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Admin Statistics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCard(
                                title = "Users",
                                value = "0",
                                icon = Icons.Default.People
                            )
                            StatCard(
                                title = "Tasks",
                                value = "0",
                                icon = Icons.Default.Assignment
                            )
                            StatCard(
                                title = "Instructions",
                                value = "0",
                                icon = Icons.Default.Description
                            )
                        }
                    }
                }
                
                // Quick Actions
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedButton(
                            onClick = { navController.navigate("user_list") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.People, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage Users")
                        }
                        
                        OutlinedButton(
                            onClick = { navController.navigate("dashboard") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Dashboard, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dashboard")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
