package com.bushy.health.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tune
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
import com.bushy.health.ui.bloub.BloubAvatar
import android.os.Build
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BushyAIScreen(
    userStats: UserStats,
    isActive: Boolean = false,
    onExpressionChange: (AvatarExpression) -> Unit,
    onAvatarClick: () -> Unit = {},
    onDeployTask: ((String, Int, TaskType) -> Unit)? = null,
    onMealLogged: ((String, Int) -> Unit)? = null,
    viewModel: BushyAIViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isUserTyping by viewModel.isUserTyping.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    
    val context = LocalContext.current
    var showCustomCamera by remember { mutableStateOf(false) }
    var showModelSettingsSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                    android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                viewModel.analyzeMealPhoto(bitmap, userStats.country) { name, calories ->
                    onMealLogged?.invoke(name, calories)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    val currentExpression = when {
        isGenerating -> AvatarExpression.THINKING
        isUserTyping -> AvatarExpression.ATTENTIVE
        else -> userStats.expression
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
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Bushy Wushy",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "⚡ ${selectedModel.displayName} (On-Device)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showModelSettingsSheet = true },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "AI Model Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
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
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer {
                                scaleX = animatedAvatarScale
                                scaleY = animatedAvatarScale
                            }
                            .clip(CircleShape)
                            .clickable { onAvatarClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        BloubAvatar(
                            expression = currentExpression,
                            visualStyle = userStats.visualStyle,
                            themeMode = userStats.themeMode,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    var showMediaMenu by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            viewModel.onUserTyping(it.isNotBlank())
                        },
                        placeholder = {
                            Text("Type a message...")
                        },
                        trailingIcon = {
                            Box(
                                modifier = Modifier.padding(end = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        showMediaMenu = true
                                    },
                                    enabled = !isGenerating,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = "Snap Meal Photo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMediaMenu,
                                    onDismissRequest = { showMediaMenu = false },
                                    shape = RoundedCornerShape(20.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Take Meal Photo", fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            showMediaMenu = false
                                            showCustomCamera = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Choose from Gallery", fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.Collections, null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            showMediaMenu = false
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    )
                                }
                            }
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
                            viewModel.sendMessage(inputText, userStats, onDeployTask)
                            inputText = ""
                        },
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
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

    if (showModelSettingsSheet) {
        ModelSettingsBottomSheet(
            selectedModel = selectedModel,
            downloadState = downloadState,
            isModelReady = { viewModel.isModelReady(it) },
            onSelectModel = { viewModel.selectModel(it) },
            onDownloadModel = { viewModel.downloadModel(it) },
            onDismiss = { showModelSettingsSheet = false }
        )
    }

    if (showCustomCamera) {
        Dialog(
            onDismissRequest = { showCustomCamera = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            BushyCameraScreen(
                onDismiss = { showCustomCamera = false },
                onPhotoCaptured = { bitmap ->
                    showCustomCamera = false
                    viewModel.analyzeMealPhoto(bitmap, userStats.country) { name, calories ->
                        onMealLogged?.invoke(name, calories)
                    }
                }
            )
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
        RoundedCornerShape(24.dp, 24.dp, 6.dp, 24.dp)
    } else {
        RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp)
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
            shape = RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsBottomSheet(
    selectedModel: LocalAiModel,
    downloadState: Map<String, ModelDownloadState>,
    isModelReady: (LocalAiModel) -> Boolean,
    onSelectModel: (LocalAiModel) -> Unit,
    onDownloadModel: (LocalAiModel) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Bushy Wushy AI Engine",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Select a local Small Language Model for 100% offline inference",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            LocalAiModel.entries.forEach { model ->
                val isSelected = model == selectedModel
                val state = downloadState[model.id] ?: ModelDownloadState.NotDownloaded
                val isReady = isModelReady(model) || state is ModelDownloadState.Ready

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSelectModel(model) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectModel(model) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    model.displayName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Start,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    model.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Start,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (model.isLocal) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        "Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (model.isLocal && isSelected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isReady) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Model ready for 100% offline inference", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            } else when (state) {
                                is ModelDownloadState.Downloading -> {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Downloading weights...", style = MaterialTheme.typography.labelSmall)
                                            Text("${state.progressPercent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { state.progressPercent / 100f },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                        )
                                    }
                                }
                                is ModelDownloadState.Error -> {
                                    Column {
                                        Text("Download failed: ${state.message}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { onDownloadModel(model) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Retry Download (${model.sizeMb} MB)")
                                        }
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = { onDownloadModel(model) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Download Model (${model.sizeMb} MB)")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
