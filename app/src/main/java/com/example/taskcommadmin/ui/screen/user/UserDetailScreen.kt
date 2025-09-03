package com.example.taskcommadmin.ui.screen.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.navigation.NavController
import com.example.taskcommadmin.data.model.User
import com.example.taskcommadmin.ui.navigation.Screen
import com.example.taskcommadmin.ui.viewmodel.UserManagementViewModel
import com.example.taskcommadmin.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.Timestamp
import java.time.OffsetDateTime
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    navController: NavController,
    userId: String,
    viewModel: UserManagementViewModel = UserManagementViewModel()
) {
    var user by remember { mutableStateOf<User?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedAddress by remember { mutableStateOf("") }
    var editedBusinessField by remember { mutableStateOf("") }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    LaunchedEffect(userId) {
        try {
            val client = SupabaseClientProvider.getClient(navController.context)
            val postgrest = client.pluginManager.getPlugin(Postgrest)
            val rows = withContext(Dispatchers.IO) {
                postgrest["profiles"].select {
                    filter { eq("id", userId) }
                    limit(1)
                }.decodeList<ProfileRow>()
            }
            val row = rows.firstOrNull()
            if (row != null) {
                val createdTs = parseSupabaseTimestamp(row.createdAt)
                user = com.example.taskcommadmin.data.model.User(
                    userId = row.id ?: "",
                    name = row.name ?: "",
                    address = "",
                    businessField = "",
                    email = row.email ?: "",
                    createdAt = createdTs
                )
                editedName = user!!.name
                editedAddress = user!!.address
                editedBusinessField = user!!.businessField
            }
        } catch (_: Exception) {}
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                // Save changes
                                user?.let { currentUser ->
                                    val updatedUser = currentUser.copy(
                                        name = editedName,
                                        address = editedAddress,
                                        businessField = editedBusinessField
                                    )
                                    viewModel.updateUser(updatedUser)
                                }
                                isEditing = false
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                        IconButton(onClick = { isEditing = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
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
                Text("Loading user details...")
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
                    Button(onClick = { /* Retry loading */ }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // User Info Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "User Information",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = editedAddress,
                                onValueChange = { editedAddress = it },
                                label = { Text("Address") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = editedBusinessField,
                                onValueChange = { editedBusinessField = it },
                                label = { Text("Business Field") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            user?.let { currentUser ->
                                InfoRow("Name", currentUser.name)
                                InfoRow("Email", currentUser.email)
                                if (currentUser.address.isNotBlank()) InfoRow("Address", currentUser.address)
                                if (currentUser.businessField.isNotBlank()) InfoRow("Business Field", currentUser.businessField)
                                InfoRow(
                                    "Joined",
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        .format(currentUser.createdAt.toDate())
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                navController.navigate(Screen.InstructionList.route + "/${userId}")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.List, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Instructions")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = { 
                                user?.let { viewModel.deleteUser(it.userId) }
                                navController.navigateUp()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete User")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Serializable
private data class ProfileRow(
    val id: String? = null,
    val email: String? = null,
    val role: String? = null,
    val name: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

private fun parseSupabaseTimestamp(value: String?): Timestamp {
    return try {
        val odt = OffsetDateTime.parse(value)
        Timestamp(java.util.Date.from(odt.toInstant()))
    } catch (_: Exception) {
        Timestamp.now()
    }
}
