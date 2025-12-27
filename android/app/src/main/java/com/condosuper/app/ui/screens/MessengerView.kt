package com.condosuper.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.condosuper.app.data.models.*
import com.condosuper.app.managers.FirebaseManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessengerView() {
    val firebaseManager = FirebaseManager.getInstance()
    val currentEmployee by firebaseManager.currentEmployee.collectAsState()
    val employees by firebaseManager.employees.collectAsState()
    val messages by firebaseManager.messages.collectAsState()
    val scope = rememberCoroutineScope()
    
    var selectedRecipientId by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    
    val conversations = remember(messages, employees, currentEmployee) {
        val employeeId = currentEmployee?.id ?: return@remember emptyList()
        val directMessages = messages
            .filter { 
                (it.recipientId == employeeId && it.senderId != employeeId) ||
                (it.senderId == employeeId && it.recipientId != null)
            }
            .groupBy { 
                if (it.senderId == employeeId) it.recipientId else it.senderId
            }
            .map { (recipientId, msgs) ->
                val recipient = employees.find { it.id == recipientId }
                Conversation(
                    recipientId = recipientId,
                    recipientName = recipient?.name ?: "Unknown",
                    lastMessage = msgs.maxByOrNull { it.timestamp },
                    unreadCount = msgs.count { !it.isRead && it.senderId != employeeId }
                )
            }
            .sortedByDescending { it.lastMessage?.timestamp ?: 0L }
        
        // Add group chat
        val groupMessages = messages
            .filter { it.recipientId == null && it.senderId != employeeId }
            .sortedByDescending { it.timestamp }
        if (groupMessages.isNotEmpty()) {
            listOf(
                Conversation(
                    recipientId = null,
                    recipientName = "Group Chat",
                    lastMessage = groupMessages.firstOrNull(),
                    unreadCount = groupMessages.count { !it.isRead }
                )
            ) + directMessages
        } else {
            directMessages
        }
    }
    
    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter { 
                it.recipientName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val currentMessages = remember(messages, selectedRecipientId, currentEmployee) {
        val employeeId = currentEmployee?.id ?: return@remember emptyList()
        if (selectedRecipientId == null) {
            // Group chat
            messages.filter { it.recipientId == null }
        } else {
            messages.filter { 
                (it.senderId == employeeId && it.recipientId == selectedRecipientId) ||
                (it.senderId == selectedRecipientId && it.recipientId == employeeId)
            }
        }.sortedBy { it.timestamp }
    }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Contacts List
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
        ) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search contacts...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            // Contacts
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredConversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        isSelected = selectedRecipientId == conversation.recipientId,
                        onClick = { selectedRecipientId = conversation.recipientId }
                    )
                }
            }
        }
        
        Divider(modifier = Modifier.width(1.dp).fillMaxHeight())
        
        // Chat Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            if (selectedRecipientId != null || filteredConversations.any { it.recipientId == null }) {
                // Chat Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filteredConversations.find { it.recipientId == selectedRecipientId }?.recipientName 
                                ?: "Group Chat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(currentMessages.reversed()) { message ->
                        MessageBubble(
                            message = message,
                            isFromCurrentUser = message.senderId == currentEmployee?.id
                        )
                    }
                }
                
                // Message Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        singleLine = false,
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && currentEmployee != null) {
                                scope.launch {
                                    firebaseManager.sendMessage(
                                        content = messageText,
                                        recipientId = selectedRecipientId,
                                        imageURL = null,
                                        videoURL = null,
                                        senderId = currentEmployee!!.id,
                                        senderName = currentEmployee!!.name
                                    )
                                    messageText = ""
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            } else {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a conversation",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.recipientName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.recipientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                conversation.lastMessage?.let { message ->
                    Text(
                        text = message.content.take(50),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (conversation.unreadCount > 0) {
                Badge(
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    Text(conversation.unreadCount.toString())
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.senderName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            if (!isFromCurrentUser) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isFromCurrentUser) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    message.imageURL?.let { url ->
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            .format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        if (isFromCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

data class Conversation(
    val recipientId: String?,
    val recipientName: String,
    val lastMessage: Message?,
    val unreadCount: Int
)

