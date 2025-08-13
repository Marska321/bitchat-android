package com.bitchat.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.model.BitchatMessage
import com.bitchat.android.model.DeliveryStatus
import com.bitchat.android.mesh.BluetoothMeshService
import com.bitchat.android.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val messages by viewModel.messages.observeAsState(emptyList())
    val connectedPeers by viewModel.connectedPeers.observeAsState(emptyList())
    val nickname by viewModel.nickname.observeAsState("")
    val selectedPrivatePeer by viewModel.selectedPrivateChatPeer.observeAsState()
    val currentChannel by viewModel.currentChannel.observeAsState()
    val privateChats by viewModel.privateChats.observeAsState(emptyMap())
    val channelMessages by viewModel.channelMessages.observeAsState(emptyMap())
    val joinedChannels by viewModel.joinedChannels.observeAsState(emptySet())
    val unreadPrivateMessages by viewModel.unreadPrivateMessages.observeAsState(emptySet())
    
    var showSidebar by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    
    val displayMessages = when {
        selectedPrivatePeer != null -> privateChats[selectedPrivatePeer] ?: emptyList()
        currentChannel != null -> channelMessages[currentChannel] ?: emptyList()
        else -> messages
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            HeaderSection(
                viewModel = viewModel,
                nickname = nickname,
                connectedPeers = connectedPeers,
                joinedChannels = joinedChannels,
                unreadPrivateMessages = unreadPrivateMessages,
                onMenuClick = { showSidebar = true },
                selectedPrivatePeer = selectedPrivatePeer,
                currentChannel = currentChannel
            )

            MessageList(
                messages = displayMessages,
                currentUserNickname = nickname,
                meshService = viewModel.meshService,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            MessageInput(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText.trim())
                        messageText = ""
                    }
                },
                nickname = nickname,
                isPrivate = selectedPrivatePeer != null
            )
        }
        
        AnimatedVisibility(
            visible = showSidebar,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            )
        ) {
            Sidebar(viewModel, onDismiss = { showSidebar = false })
        }
    }
}

@Composable
private fun HeaderSection(
    viewModel: ChatViewModel,
    nickname: String,
    connectedPeers: List<String>,
    joinedChannels: Set<String>,
    unreadPrivateMessages: Set<String>,
    onMenuClick: () -> Unit,
    selectedPrivatePeer: String?,
    currentChannel: String?
) {
    val colorScheme = MaterialTheme.colorScheme
    
    TopAppBar(
        title = {
            if (selectedPrivatePeer != null) {
                // Private chat header
                val peerNickname = viewModel.meshService.getPeerNicknames()[selectedPrivatePeer] ?: "Unknown"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.endPrivateChat() }
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    UserAvatar(nickname = peerNickname, size = 32.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(peerNickname, style = MaterialTheme.typography.titleMedium)
                }
            } else if (currentChannel != null) {
                // Channel header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.switchToChannel(null) }
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Text("#$currentChannel", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                }
            } else {
                // Main header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("bitchat*", style = MaterialTheme.typography.headlineSmall, color = colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    NicknameEditor(value = nickname, onValueChange = viewModel::setNickname)
                }
            }
        },
        actions = {
            if (selectedPrivatePeer != null) {
                // Favorite button in private chat
                val isFavorite = selectedPrivatePeer?.let { viewModel.isFavorite(it) } ?: false
                IconButton(onClick = { selectedPrivatePeer?.let { viewModel.toggleFavorite(it) } }) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            IconButton(onClick = onMenuClick) {
                Badge(
                    containerColor = colorScheme.secondary,
                    contentColor = colorScheme.onSecondary,
                    modifier = Modifier.offset(x = (-8).dp, y = 8.dp),
                    content = {
                        val unreadCount = unreadPrivateMessages.size + connectedPeers.filter { peerId ->
                            viewModel.unreadChannelMessages.value?.get(
                                viewModel.meshService.getPeerNicknames()[peerId] ?: ""
                            ) ?: 0 > 0
                        }.size
                        if (unreadCount > 0) {
                            Text(text = unreadCount.toString())
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = colorScheme.onSurface
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.onSurface
        )
    )
}

@Composable
private fun MessageList(
    messages: List<BitchatMessage>,
    currentUserNickname: String,
    meshService: BluetoothMeshService,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            MessageItem(
                message = message,
                currentUserNickname = currentUserNickname,
                meshService = meshService
            )
        }
    }
}

@Composable
private fun MessageItem(
    message: BitchatMessage,
    currentUserNickname: String,
    meshService: BluetoothMeshService
) {
    val colorScheme = MaterialTheme.colorScheme
    val isSystemMessage = message.sender == "system"
    val isMyMessage = message.sender == currentUserNickname
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar for incoming messages
        if (!isMyMessage && !isSystemMessage) {
            UserAvatar(
                nickname = message.sender,
                size = 32.dp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Message bubble
        Column(
            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Sender info for incoming messages
            if (!isMyMessage && !isSystemMessage) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelSmall,
                    color = getSenderColor(message, meshService),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // The chat bubble itself
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isSystemMessage -> colorScheme.surface
                    isMyMessage -> colorScheme.primary
                    else -> colorScheme.primaryContainer
                },
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isSystemMessage) message.content else message.content,
                        color = if (isMyMessage) colorScheme.onPrimary else colorScheme.onBackground
                    )
                }
            }

            // Timestamp and status for my messages
            if (isMyMessage) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(4.dp))
                    message.deliveryStatus?.let { status ->
                        DeliveryStatusIcon(status = status)
                    }
                }
            } else if (!isSystemMessage) {
                // Timestamp for incoming messages
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    nickname: String,
    isPrivate: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(colorScheme.surface, RoundedCornerShape(24.dp))
            .border(1.dp, colorScheme.primary, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
            cursorBrush = SolidColor(colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                onSend()
                keyboardController?.hide()
            }),
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = if (isPrivate) "Type a private message..." else "Type a message...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank(),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Send Message",
                tint = if (value.isNotBlank()) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun Sidebar(viewModel: ChatViewModel, onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val connectedPeers by viewModel.connectedPeers.observeAsState(emptyList())
    val joinedChannels by viewModel.joinedChannels.observeAsState(emptySet())
    val currentChannel by viewModel.currentChannel.observeAsState()
    val nickname by viewModel.nickname.observeAsState("")
    val peerNicknames = viewModel.meshService.getPeerNicknames()
    val peerRSSI = viewModel.meshService.getPeerRSSI()
    val myPeerID = viewModel.meshService.myPeerID

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .background(colorScheme.surface)
                .align(Alignment.CenterEnd)
                .clickable { /* Prevents closing when clicking inside */ }
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "My Network",
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Channels Section
            if (joinedChannels.isNotEmpty()) {
                Text(
                    text = "CHANNELS",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                joinedChannels.forEach { channel ->
                    SidebarItem(
                        text = "#$channel",
                        onClick = {
                            viewModel.switchToChannel(channel)
                            onDismiss()
                        },
                        isSelected = channel == currentChannel,
                        colorScheme = colorScheme
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // People Section
            Text(
                text = "PEOPLE",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            connectedPeers.sortedBy { peerNicknames[it] ?: it }.forEach { peerID ->
                val displayName = peerNicknames[peerID] ?: peerID
                SidebarItem(
                    text = displayName,
                    onClick = {
                        viewModel.startPrivateChat(peerID)
                        onDismiss()
                    },
                    isSelected = peerID == selectedPrivatePeer,
                    colorScheme = colorScheme,
                    isFavorite = viewModel.isFavorite(peerID),
                    rssi = peerRSSI[peerID] ?: 0
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(
    text: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    colorScheme: ColorScheme,
    isFavorite: Boolean = false,
    rssi: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) colorScheme.primaryContainer else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rssi != null) {
            val signalStrength = when {
                rssi >= -50 -> 3
                rssi >= -70 -> 2
                rssi >= -90 -> 1
                else -> 0
            }
            SignalStrengthIndicator(strength = signalStrength, color = getRSSIColor(rssi))
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        
        if (isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Favorite",
                tint = colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun UserAvatar(nickname: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val initial = nickname.firstOrNull()?.uppercase() ?: "?"
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colorScheme.secondary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = (size.value / 2).sp,
                fontWeight = FontWeight.Bold
            ),
            color = colorScheme.onSecondary
        )
    }
}

@Composable
private fun SignalStrengthIndicator(strength: Int, color: Color) {
    Row(
        modifier = Modifier.width(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        repeat(3) { index ->
            val isFilled = index < strength
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = (4 + index * 2).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color.copy(alpha = if (isFilled) 1f else 0.3f))
            )
            if (index < 2) Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

@Composable
private fun getSenderColor(message: BitchatMessage, meshService: BluetoothMeshService): Color {
    val rssi = message.senderPeerID?.let { meshService.getPeerRSSI()[it] } ?: -60
    return getRSSIColor(rssi)
}

@Composable
private fun getRSSIColor(rssi: Int): Color {
    return when {
        rssi >= -50 -> Color(0xFF00FF00) // Bright green
        rssi >= -60 -> Color(0xFF80FF00) // Green-yellow
        rssi >= -70 -> Color(0xFFFFFF00) // Yellow
        rssi >= -80 -> Color(0xFFFF8000) // Orange
        else -> Color(0xFFFF4444) // Red
    }
}

@Composable
private fun NicknameEditor(value: String, onValueChange: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "@",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.primary.copy(alpha = 0.8f)
        )
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = colorScheme.primary,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.widthIn(max = 100.dp)
        )
    }
}

@Composable
private fun DeliveryStatusIcon(status: DeliveryStatus) {
    val colorScheme = MaterialTheme.colorScheme
    
    val iconColor = when (status) {
        is DeliveryStatus.Sending -> colorScheme.onSurface.copy(alpha = 0.6f)
        is DeliveryStatus.Sent -> colorScheme.onSurface.copy(alpha = 0.6f)
        is DeliveryStatus.Delivered -> colorScheme.primary
        is DeliveryStatus.Read -> Color(0xFF007AFF)
        is DeliveryStatus.Failed -> colorScheme.error
        is DeliveryStatus.PartiallyDelivered -> colorScheme.onSurface.copy(alpha = 0.6f)
    }
    
    val icon = when (status) {
        is DeliveryStatus.Sending -> "○"
        is DeliveryStatus.Sent -> "✓"
        is DeliveryStatus.Delivered -> "✓✓"
        is DeliveryStatus.Read -> "✓✓"
        is DeliveryStatus.Failed -> "⚠"
        is DeliveryStatus.PartiallyDelivered -> "✓"
    }

    Text(text = icon, fontSize = 10.sp, color = iconColor)
}

