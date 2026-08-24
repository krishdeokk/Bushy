package com.bushy.health.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.bushy.health.*
import android.os.Build
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BushyAIScreen(
    userStats: UserStats,
    isActive: Boolean = false,
    onExpressionChange: (AvatarExpression) -> Unit,
    onAvatarClick: () -> Unit = {},
    viewModel: BushyAIViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isUserTyping by viewModel.isUserTyping.collectAsState()
    
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val currentExpression = when {
        isGenerating -> AvatarExpression.THINKING
        isUserTyping -> AvatarExpression.ATTENTIVE
        else -> userStats.expression
    }

    val gifName = remember(userStats.avatarType, currentExpression, userStats.visualStyle) {
        val expr = currentExpression.name.lowercase()
        if (userStats.visualStyle == VisualStyle.MONOCHROME) {
            "mono_${expr}"
        } else {
            val gender = userStats.avatarType.name.lowercase()
            "${gender}_${expr}"
        }
    }
    
    val gifResId = remember(gifName) {
        val id = context.resources.getIdentifier(gifName, "drawable", context.packageName)
        if (id != 0) id else null
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // character reactions
    LaunchedEffect(isUserTyping, isGenerating) {
        when {
            isGenerating -> onExpressionChange(AvatarExpression.THINKING)
            isUserTyping -> onExpressionChange(AvatarExpression.ATTENTIVE)
            else -> onExpressionChange(AvatarExpression.NEUTRAL)
        }
    }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    var avatarScaleTrigger by remember { mutableStateOf(0f) }
    val animatedAvatarScale by animateFloatAsState(
        targetValue = if (avatarScaleTrigger > 0f) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AvatarBounce"
    )

    LaunchedEffect(isActive) {
        if (isActive) {
            avatarScaleTrigger = 0f
            kotlinx.coroutines.delay(100)
            avatarScaleTrigger = 1f
        } else {
            avatarScaleTrigger = 0f
        }
    }

    // PERMANENT FIX: Simple Box Root Layout
    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. CHAT CONTENT (Layered behind everything else)
        Column(modifier = Modifier.fillMaxSize()) {
            // Spacer to clear the fixed header area
            Spacer(modifier = Modifier.statusBarsPadding().padding(top = 100.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                val isImeVisible = WindowInsets.isImeVisible
                if (messages.isEmpty() && !isGenerating && !isImeVisible) {
                    // EmptyStateMessage is now handled separately to be perfectly centered
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        // Bottom padding ensures the last message is visible above the input bar when keyboard is closed
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages) { message ->
                            ChatBubble(message)
                        }
                        
                        if (isGenerating) {
                            item {
                                GeneratingIndicator()
                            }
                        }
                    }
                }
            }
        }

        // NEW: Perfectly Centered Empty State
        val isImeVisible = WindowInsets.isImeVisible
        if (messages.isEmpty() && !isGenerating && !isImeVisible) {
            EmptyStateMessage()
        }

        // 2. FIXED HEADER (Aligned Top)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Bushy Wushy",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Powered by Gemini",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // 3. MESSAGE INPUT
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // Avatar
                    val systemInDarkTheme = isSystemInDarkTheme()
                    val isDark = remember(userStats.themeMode, systemInDarkTheme) {
                        when (userStats.themeMode) {
                            ThemeMode.LIGHT -> false
                            ThemeMode.DARK -> true
                            ThemeMode.SYSTEM -> systemInDarkTheme
                        }
                    }
                    val avatarBgColor = when {
                        userStats.visualStyle == VisualStyle.MATERIAL3 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        userStats.visualStyle == VisualStyle.MONOCHROME && isDark -> Color.White
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer {
                                scaleX = animatedAvatarScale
                                scaleY = animatedAvatarScale
                            }
                            .clip(CircleShape)
                            .clickable { onAvatarClick() }
                            .background(
                                avatarBgColor,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (gifResId != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(gifResId)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(0.9f),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            viewModel.onUserTyping(it.isNotBlank())
                        },
                        placeholder = {
                            Text("Type a message...")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    FilledIconButton(
                        onClick = {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        modifier = Modifier.size(52.dp),
                        enabled = inputText.isNotBlank() && !isGenerating
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Welcome to Bushy Wushy",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Hi! I am Bushy Wushy, your AI guide. Ask me anything about your fitness journey, health stats, or how to use the app!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChatBubble(message: BushyAIMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    val shape = if (message.isUser) {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun GeneratingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Bushy is thinking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
