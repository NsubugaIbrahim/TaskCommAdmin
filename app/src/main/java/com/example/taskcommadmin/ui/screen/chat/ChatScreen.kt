package com.example.taskcommadmin.ui.screen.chat

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
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
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    
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
                                color = Color.White.copy(alpha = 0.8f)
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
            Column {
                // Reply indicator above input
                if (replyToMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Reply, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to ${if (replyToMessage!!.senderRole == "admin") adminDisplayName else userName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = replyToMessage!!.text.take(50) + if (replyToMessage!!.text.length > 50) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(onClick = { replyToMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply")
                            }
                        }
                    }
                }
                
            ChatInputBar(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                            val finalText = if (replyToMessage != null) {
                                val senderName = if (replyToMessage!!.senderRole == "admin") adminDisplayName else userName
                                "↩️ $senderName: \"${replyToMessage!!.text.take(30)}...\"\n\n$messageText"
                            } else messageText
                        viewModel.sendTextMessage(
                                navController.context,
                            taskId = taskId,
                                text = finalText,
                                senderId = adminUid,
                                senderName = adminDisplayName
                        )
                        messageText = ""
                            replyToMessage = null
                    }
                },
                onAttachFile = { /* Handle file attachment */ }
            )
            }
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
                // Task name bubble at top
                item(key = "task-header") {
                    if (taskTitle.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = taskTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Group messages by date
                val groupedMessages = messages.groupBy { 
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.timestamp.toDate())
                }
                
                groupedMessages.forEach { (date, messagesForDate) ->
                    // Date header
                    item(key = "date-$date") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = formatDateHeader(date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Messages for this date
                    items(messagesForDate, key = { it.messageId }) { message ->
                    ChatMessageItem(
                        message = message,
                            isFromAdmin = message.senderRole == "admin",
                            onLongClick = {
                                selectedMessage = message
                                showContextMenu = true
                            }
                        )
                    }
                }
            }
        }
        
        // Context menu for message actions
        if (showContextMenu && selectedMessage != null) {
            AlertDialog(
                onDismissRequest = { showContextMenu = false },
                title = { Text("Message Actions") },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                replyToMessage = selectedMessage
                                showContextMenu = false
                            }
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reply")
                        }
                        TextButton(
                            onClick = {
                                editText = selectedMessage!!.text
                                showEditDialog = true
                                showContextMenu = false
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Message")
                        }
                        TextButton(
                            onClick = {
                                viewModel.deleteMessage(navController.context, selectedMessage!!.messageId)
                                showContextMenu = false
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Message")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showContextMenu = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Edit dialog
        if (showEditDialog && selectedMessage != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Message") },
                text = {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (editText.isNotBlank()) {
                                viewModel.editMessage(navController.context, selectedMessage!!.messageId, editText)
                            }
                            showEditDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Reply indicator
        if (replyToMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Reply, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = replyToMessage!!.text.take(50) + "...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    IconButton(onClick = { replyToMessage = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel reply")
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@kotlinx.serialization.Serializable
private data class TaskHeaderRow(
    val id: String? = null,
    @kotlinx.serialization.SerialName("instruction_id") val instructionId: String? = null,
    @kotlinx.serialization.SerialName("title") val title: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@kotlinx.serialization.Serializable
private data class InstructionHeaderRow(
    val id: String? = null,
    @kotlinx.serialization.SerialName("user_id") val userId: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@kotlinx.serialization.Serializable
private data class ProfileHeaderRow(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isFromAdmin: Boolean,
    onLongClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
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
        Surface(
            color = bubbleColor, 
            shape = shape,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onDoubleClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
        ) {
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
                // Check if this is a reply message
                val isReply = message.text.startsWith("↩️")
                val (replyPart, actualMessage) = if (isReply) {
                    val parts = message.text.split("\n\n", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else "" to message.text
                } else {
                    "" to message.text
                }
                
                // Remove "(edited)" from the actual message text
                val cleanMessage = actualMessage.replace(" (edited)", "")
                
                // Show reply context if it's a reply
                if (isReply && replyPart.isNotBlank()) {
                    Surface(
                        color = if (isFromAdmin) 
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f) 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                Text(
                            text = replyPart.removePrefix("↩️ "),
                            style = MaterialTheme.typography.bodySmall,
                            color = bubbleText.copy(alpha = 0.7f),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = cleanMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bubbleText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = bubbleText.copy(alpha = 0.8f)
                    )
                    // Show edited flag if message was edited
                    if (message.text.contains(" (edited)")) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "edited",
                            style = MaterialTheme.typography.labelSmall,
                            color = bubbleText.copy(alpha = 0.6f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
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

fun formatDateHeader(dateString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
        
        when (dateString) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> date?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it) } ?: dateString
        }
    } catch (_: Exception) {
        dateString
    }
}