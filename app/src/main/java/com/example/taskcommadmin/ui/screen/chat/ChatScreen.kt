package com.example.taskcommadmin.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.navigation.NavController
import com.example.taskcommadmin.data.model.ChatMessage
import com.example.taskcommadmin.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.example.taskcommadmin.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.gotrue.Auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    taskId: String,
    viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var userName by remember { mutableStateOf("") }
    var taskTitle by remember { mutableStateOf("") }
    var adminUid by remember { mutableStateOf("") }
    var adminDisplayName by remember { mutableStateOf("Admin") }
    
    LaunchedEffect(taskId) {
        Log.d("AdminChatScreen", "Opening chat for task=" + taskId)
        viewModel.loadMessages(navController.context, taskId)
        // Fetch end-user name for header (task -> instruction -> profiles)
        try {
            val client = SupabaseClientProvider.getClient(navController.context)
            val postgrest = client.pluginManager.getPlugin(Postgrest)
            val auth = client.pluginManager.getPlugin(Auth)
            val taskRow = withContext(Dispatchers.IO) {
                postgrest["tasks"].select {
                    filter { eq("id", taskId) }
                    limit(1)
                }.decodeList<TaskHeaderRow>()
            }.firstOrNull()
            taskTitle = taskRow?.title ?: ""
            val instructionId = taskRow?.instructionId
            if (!instructionId.isNullOrBlank()) {
                val userId = withContext(Dispatchers.IO) {
                    postgrest["instructions"].select {
                        filter { eq("id", instructionId) }
                        limit(1)
                    }.decodeList<InstructionHeaderRow>()
                }.firstOrNull()?.userId
                if (!userId.isNullOrBlank()) {
                    val profile = withContext(Dispatchers.IO) {
                        postgrest["profiles"].select {
                            filter { eq("id", userId) }
                            limit(1)
                        }.decodeList<ProfileHeaderRow>()
                    }.firstOrNull()
                    userName = (profile?.name ?: profile?.email ?: "User").trim()
                    Log.d("AdminChatScreen", "Header userName=" + userName)
                }
            }
            // Resolve admin uid and display name for sending
            val uid = auth.currentSessionOrNull()?.user?.id
            if (!uid.isNullOrBlank()) {
                adminUid = uid
                val adminProfile = withContext(Dispatchers.IO) {
                    postgrest["profiles"].select {
                        filter { eq("id", uid) }
                        limit(1)
                    }.decodeList<ProfileHeaderRow>()
                }.firstOrNull()
                adminDisplayName = (adminProfile?.name ?: adminProfile?.email ?: "Admin").trim()
                Log.d("AdminChatScreen", "Admin uid=" + adminUid + ", name=" + adminDisplayName)
            }
        } catch (_: Exception) { }
    }
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (userName.isBlank()) "Chat" else userName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (taskTitle.isNotBlank()) {
                            Text(
                                text = "Task: " + taskTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Export chat */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4B2E83),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        // TODO: wire real admin id/name from AuthViewModel
                        viewModel.sendTextMessage(
                            navController.context,
                            taskId = taskId,
                            text = messageText,
                            senderId = adminUid,
                            senderName = adminDisplayName
                        )
                        messageText = ""
                    }
                },
                onAttachFile = { /* Handle file attachment */ }
            )
        }
    ) { paddingValues ->
        if (isLoading && messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading messages...")
            }
        } else if (error != null && messages.isEmpty()) {
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
                    Button(onClick = { viewModel.loadMessages(navController.context, taskId) }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item(key = "spacer-top") { Spacer(modifier = Modifier.height(1.dp)) }
                items(messages, key = { it.messageId }) { message ->
                    ChatMessageItem(
                        message = message,
                        isFromAdmin = message.senderRole == "admin"
                    )
                }
                item(key = "spacer-bottom") { Spacer(modifier = Modifier.height(1.dp)) }
            }
        }
    }
}

@kotlinx.serialization.Serializable
private data class TaskHeaderRow(
    val id: String? = null,
    @kotlinx.serialization.SerialName("instruction_id") val instructionId: String? = null,
    @kotlinx.serialization.SerialName("title") val title: String? = null
)

@kotlinx.serialization.Serializable
private data class InstructionHeaderRow(
    val id: String? = null,
    @kotlinx.serialization.SerialName("user_id") val userId: String? = null
)

@kotlinx.serialization.Serializable
private data class ProfileHeaderRow(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null
)

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isFromAdmin: Boolean
) {
    val alignment = if (isFromAdmin) Alignment.End else Alignment.Start
    val bubbleColor = if (isFromAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val bubbleText = if (isFromAdmin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromAdmin) Arrangement.End else Arrangement.Start
    ) {
        val shape = if (isFromAdmin) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
        } else {
            RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
        }
        val screenWidth = LocalConfiguration.current.screenWidthDp
        val maxBubbleWidth = (screenWidth * 0.7f).dp
        Surface(color = bubbleColor, shape = shape) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (message.senderName.isNotBlank() && !isFromAdmin) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = bubbleText.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = message.text + "  ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = bubbleText
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = bubbleText.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachFile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttachFile) {
                Icon(Icons.Default.Share, contentDescription = "Attach file")
            }
            
            TextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
                placeholder = { Text("Type a message…") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            FilledIconButton(
                onClick = onSendMessage,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
