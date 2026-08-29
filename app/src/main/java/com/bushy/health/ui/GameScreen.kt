package com.bushy.health.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.bushy.health.*
import com.bushy.health.ui.bloub.BloubAvatar
import com.bushy.health.ui.theme.MaterialExpressiveShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    onRequestPermissions: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (!uiState.isSetupComplete) {
        SetupScreen(viewModel = viewModel, onRequestPermissions = onRequestPermissions)
    } else {
        GameMainContent(uiState, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameMainContent(uiState: UserStats, viewModel: GameViewModel) {
    var showAvatarSelection by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val performSubtleClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }

    val pagerState = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val graphicsLayer = rememberGraphicsLayer()
    val storyGraphicsLayer = rememberGraphicsLayer()

    Scaffold(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(onPress = { 
                    viewModel.onScreenTouch() 
                })
            },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val fraction = pageOffset.coerceIn(-1f, 1f).let { if (it < 0) -it else it }
                            
                            this.alpha = 1f - fraction
                            val scale = 0.95f + (1f - fraction) * 0.05f
                            this.scaleX = scale
                            this.scaleY = scale
                        }
                ) {
                    if (page == 0) {
                        var homeTouchPosition by remember { mutableStateOf<Offset?>(null) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                            val position = event.changes.firstOrNull()?.position
                                            val pressed = event.changes.any { it.pressed }
                                            homeTouchPosition = if (pressed && position != null) position else null
                                        }
                                    }
                                }
                        ) {
                            HomeScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                externalTouchPositionInWindow = homeTouchPosition
                            )
                        }
                    } else if (page == 1) {
                        TasksScreen(
                            tasks = uiState.tasks, 
                            onTaskHold = { viewModel.incrementTask(it) },
                            onTaskReset = { viewModel.resetTask(it) },
                            onAddTask = { showAddTaskDialog = true }
                        )
                    } else {
                        // Bushy Wushy AI Tab
                        BushyAIScreen(
                            userStats = uiState,
                            isActive = pagerState.currentPage == 2,
                            onExpressionChange = { viewModel.setExpressionDirectly(it) },
                            onAvatarClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                scope.launch { 
                                    pagerState.animateScrollToPage(
                                        page = 0,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) 
                                }
                            }
                        )
                    }
                }
            }

            // Compact Header Actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    if (pagerState.currentPage == 0) {
                        val needsSync = uiState.syncMessage != null
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Box {
                                Icon(
                                    Icons.Default.MoreVert, 
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                if (needsSync) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showOptionsMenu && pagerState.currentPage == 0,
                        onDismissRequest = { showOptionsMenu = false },
                        offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Sync Activity", fontWeight = FontWeight.Medium)
                                    if (uiState.syncMessage != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(MaterialTheme.colorScheme.error, CircleShape)
                                        )
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = { 
                                showOptionsMenu = false
                                viewModel.refreshStats() 
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        DropdownMenuItem(
                            text = { Text("Switch Hero", fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Face, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = { 
                                showOptionsMenu = false
                                showAvatarSelection = true 
                            }
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        
                        Text(
                            "Visual Style",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        VibeMenuItem(
                            label = "Vibrant (M3)",
                            icon = Icons.Default.AutoAwesome,
                            selected = uiState.visualStyle == VisualStyle.MATERIAL3,
                            onClick = { 
                                showOptionsMenu = false
                                viewModel.setVisualStyle(VisualStyle.MATERIAL3) 
                            }
                        )
                        VibeMenuItem(
                            label = "Minimal (Mono)",
                            icon = Icons.Default.FilterBAndW,
                            selected = uiState.visualStyle == VisualStyle.MONOCHROME,
                            onClick = { 
                                showOptionsMenu = false
                                viewModel.setVisualStyle(VisualStyle.MONOCHROME) 
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        
                        Text(
                            "Theme Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        ThemeMenuItem(
                            label = "Light",
                            icon = Icons.Default.LightMode,
                            selected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { 
                                showOptionsMenu = false
                                viewModel.setThemeMode(ThemeMode.LIGHT) 
                            }
                        )
                        ThemeMenuItem(
                            label = "Dark",
                            icon = Icons.Default.DarkMode,
                            selected = uiState.themeMode == ThemeMode.DARK,
                            onClick = { 
                                showOptionsMenu = false
                                viewModel.setThemeMode(ThemeMode.DARK) 
                            }
                        )
                        ThemeMenuItem(
                            label = "Auto",
                            icon = Icons.Default.SettingsSuggest,
                            selected = uiState.themeMode == ThemeMode.SYSTEM,
                            onClick = { 
                                showOptionsMenu = false
                                viewModel.setThemeMode(ThemeMode.SYSTEM) 
                            }
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Reset Progress", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { 
                                showOptionsMenu = false
                                performSubtleClick()
                                viewModel.resetAllStats() 
                            }
                        )
                    }
                }

                if (pagerState.currentPage == 0) {
                    IconButton(
                        onClick = { showShareMenu = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            Icons.Default.Share, 
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // DYNAMIC NAVIGATION PILL
            val isBushyAITab = pagerState.currentPage == 2
            val isKeyboardVisible = WindowInsets.isImeVisible
            
            val pillPadding by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isBushyAITab) 120.dp else 32.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "PillPadding"
            )
            val pillAlignment = if (isBushyAITab) Alignment.BottomEnd else Alignment.BottomCenter

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = pillPadding, end = if (isBushyAITab) 24.dp else 0.dp),
                contentAlignment = pillAlignment
            ) {
                if (!isKeyboardVisible && !isBushyAITab) {
                    Surface(
                        onClick = { /* Pill items handle their own clicks */ },
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (uiState.visualStyle == VisualStyle.MONOCHROME) 
                                MaterialTheme.colorScheme.onBackground
                            else 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            .wrapContentWidth()
                            .height(72.dp),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(6.dp)
                                .wrapContentWidth()
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            PillTabItem(
                                selected = pagerState.currentPage == 0,
                                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                                label = "Home",
                                icon = Icons.Default.Home,
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                            PillTabItem(
                                selected = pagerState.currentPage == 1,
                                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                                label = "Tasks",
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                            PillTabItem(
                                selected = pagerState.currentPage == 2,
                                onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                                label = "Bushy",
                                icon = Icons.Default.AutoAwesome,
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showChangelog) {
        ChangelogDialog(
            onDismiss = { viewModel.dismissChangelog() }
        )
    }

    if (showAvatarSelection) {
        AvatarSelectionDialog(
            onDismiss = { showAvatarSelection = false },
            onSelect = {
                viewModel.setAvatar(it)
                showAvatarSelection = false
            }
        )
    }

    // UNIVERSAL FIX: HIDDEN STORY RENDER LAYER
    // Only record when sharing story to avoid per-frame overhead
    var isSharingStory by remember { mutableStateOf(false) }

    if (showShareMenu) {
        ShareMenuDialog(
            onDismiss = { showShareMenu = false },
            onShareText = {
                com.bushy.health.ShareUtils.shareProgress(context, uiState)
                showShareMenu = false
            },
            onShareStory = {
                isSharingStory = true
            }
        )
    }

    if (isSharingStory) {
        LaunchedEffect(Unit) {
            // Give time for one frame to render and record
            kotlinx.coroutines.delay(100)
            try {
                val bitmap = storyGraphicsLayer.toImageBitmap().asAndroidBitmap()
                com.bushy.health.ShareUtils.shareBitmap(context, bitmap, "My Bushy Story!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isSharingStory = false
            showShareMenu = false
        }
    }

    Box(
        modifier = Modifier
            .size(1.dp)
            .graphicsLayer {
                this.translationX = 20000f // Move way off-screen
            }
            .drawWithCache {
                onDrawWithContent {
                    if (isSharingStory) {
                        storyGraphicsLayer.record(androidx.compose.ui.unit.IntSize(1080, 1920)) {
                            this@onDrawWithContent.drawContent()
                        }
                    }
                }
            }
    ) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(3f)
        ) {
            Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) { 
                StoryShareCard(stats = uiState)
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAdd = { title, target, type ->
                viewModel.addNewTask(title, target, type)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun VibeMenuItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        leadingIcon = { 
            Icon(
                icon, 
                null, 
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        trailingIcon = {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline
                )
            )
        },
        onClick = onClick
    )
}

@Composable
fun ThemeMenuItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        leadingIcon = { 
            Icon(
                icon, 
                null, 
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        trailingIcon = {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline
                )
            )
        },
        onClick = onClick
    )
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, Int, com.bushy.health.TaskType) -> Unit) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(com.bushy.health.TaskType.PUSHUPS) }

    val presets = listOf(
        Triple("Pushups (Chest)", 20, com.bushy.health.TaskType.PUSHUPS),
        Triple("Squats (Legs)", 15, com.bushy.health.TaskType.PUSHUPS),
        Triple("Bicep Curls (Arms)", 12, com.bushy.health.TaskType.PUSHUPS),
        Triple("Pullups (Back)", 10, com.bushy.health.TaskType.PUSHUPS),
        Triple("Crunches (Abs)", 25, com.bushy.health.TaskType.PUSHUPS)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        title = { Text("New Mission", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Quick Presets:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets.size) { index ->
                        val (pTitle, pTarget, pType) = presets[index]
                        SuggestionChip(
                            onClick = { 
                                title = pTitle
                                target = pTarget.toString()
                                type = pType
                            },
                            label = { Text(pTitle) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Mission Title") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { if (it.all { char -> char.isDigit() }) target = it },
                    label = { Text("Goal Target") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val targetInt = target.toIntOrNull() ?: 10
                    onAdd(title, targetInt, type) 
                },
                enabled = title.isNotBlank() && target.isNotBlank()
            ) {
                Text("Deploy Mission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ShareMenuDialog(
    onDismiss: () -> Unit,
    onShareText: () -> Unit,
    onShareStory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        title = { Text("Share Your Progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onShareStory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Share Hero Story (Image)")
                }
                OutlinedButton(
                    onClick = { onShareText() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TextSnippet, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Share Stats as Text")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LevelHeader(stats: UserStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(width = 80.dp, height = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "LVL ${stats.level}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                "${stats.xpInCurrentLevel} / ${stats.xpRequiredForNextLevel} XP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LinearProgressIndicator(
            progress = { stats.levelProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun HomeScreen(
    uiState: UserStats,
    viewModel: GameViewModel,
    externalTouchPositionInWindow: Offset? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            AvatarSection(
                type = uiState.avatarType,
                expression = uiState.expression,
                visualStyle = uiState.visualStyle,
                themeMode = uiState.themeMode,
                userName = uiState.userName,
                externalTouchPositionInWindow = externalTouchPositionInWindow,
                onAvatarTap = { viewModel.onAvatarTap() },
                onAvatarDoubleTap = { viewModel.onAvatarDoubleTap() }
            )
        }

        item {
            LevelHeader(uiState)
        }

        if (uiState.syncMessage != null) {
            item {
                SyncStatusCard(uiState.syncMessage)
            }
        }

        item {
            StatsGrid(uiState)
        }
    }
}

@Composable
fun TasksScreen(tasks: List<com.bushy.health.HealthTask>, onTaskHold: (String) -> Unit, onTaskReset: (String) -> Unit, onAddTask: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    "Daily Missions",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(tasks.size) { index ->
                val task = tasks[index]
                TaskCard(task, onHold = { onTaskHold(task.id) }, onReset = { onTaskReset(task.id) })
            }
        }

        // MEGA ADD BUTTON
        LargeFloatingActionButton(
            onClick = onAddTask,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 140.dp, end = 24.dp)
                .size(92.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialExpressiveShapes.cookie9
        ) {
            Icon(Icons.Default.Add, "Add Mission", modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun PillTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    selectedColor: Color
) {
    Surface(
        onClick = onClick,
        color = if (selected) selectedColor else Color.Transparent,
        contentColor = if (selected) contentColorFor(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
        modifier = modifier.fillMaxHeight()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    label, 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
fun TaskCard(task: com.bushy.health.HealthTask, onHold: () -> Unit, onReset: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val containerColor = if (task.isCompleted) 
        MaterialTheme.colorScheme.primaryContainer 
    else MaterialTheme.colorScheme.surfaceContainerHigh
    
    val textBgColor = if (task.isCompleted)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    var isPressing by remember { mutableStateOf(false) }
    
    val animatedProgress by animateFloatAsState(
        targetValue = task.current.toFloat() / task.target.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "TaskProgress"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressing) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "TaskCardScale"
    )

    val cornerRadius by animateDpAsState(
        targetValue = when {
            isPressing -> 18.dp
            task.isCompleted -> 32.dp
            else -> 36.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "TaskCardCorner"
    )

    LaunchedEffect(isPressing) {
        if (isPressing && !task.isCompleted) {
            while (isPressing && !task.isCompleted) {
                onHold()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(100)
            }
        }
    }

    val taskIcon = remember(task.title) {
        val title = task.title.lowercase()
        when {
            title.contains("walk") || title.contains("step") -> Icons.AutoMirrored.Filled.DirectionsWalk
            title.contains("run") || title.contains("cardio") -> Icons.AutoMirrored.Filled.DirectionsRun
            title.contains("pushup") || title.contains("chest") || title.contains("bench") -> Icons.Default.FitnessCenter
            title.contains("bicep") || title.contains("curl") || title.contains("arm") -> Icons.Default.SportsGymnastics
            title.contains("leg") || title.contains("squat") || title.contains("lunges") -> Icons.AutoMirrored.Filled.DirectionsRun
            title.contains("pullup") || title.contains("back") || title.contains("row") -> Icons.Default.VerticalAlignTop
            title.contains("abs") || title.contains("core") || title.contains("situp") -> Icons.Default.SelfImprovement
            title.contains("bike") || title.contains("cycle") -> Icons.AutoMirrored.Filled.DirectionsBike
            title.contains("swim") -> Icons.Default.Pool
            else -> Icons.Default.Timer
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(task.id, task.isCompleted) {
                    if (!task.isCompleted) {
                        detectTapGestures(
                            onPress = {
                                isPressing = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                try {
                                    awaitRelease()
                                } finally {
                                    isPressing = false
                                }
                            }
                        )
                    }
                },
            shape = RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = if (task.isCompleted) 16.dp else cornerRadius,
                bottomEnd = cornerRadius,
                bottomStart = cornerRadius
            ),
            colors = CardDefaults.elevatedCardColors(containerColor = containerColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else taskIcon,
                                contentDescription = null,
                                tint = if (task.isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColorFor(containerColor),
                        modifier = Modifier.weight(1f)
                    )

                    FilledTonalIconButton(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReset() 
                        },
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = textBgColor,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            if (task.isCompleted) "MISSION COMPLETE!" else "${task.current} / ${task.target} progressed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColorFor(containerColor).copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarSection(
    type: AvatarType,
    expression: AvatarExpression,
    visualStyle: VisualStyle,
    themeMode: ThemeMode,
    userName: String,
    externalTouchPositionInWindow: Offset? = null,
    onAvatarTap: () -> Unit,
    onAvatarDoubleTap: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val performSubtleClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { 
                            performSubtleClick()
                            onAvatarTap() 
                        },
                        onDoubleTap = { 
                            performSubtleClick()
                            onAvatarDoubleTap() 
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            BloubAvatar(
                expression = expression,
                visualStyle = visualStyle,
                themeMode = themeMode,
                externalTouchPositionInWindow = externalTouchPositionInWindow,
                modifier = Modifier.size(360.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (userName.isNotBlank()) "${userName}'s Bushy" else "Bushy",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StatsGrid(stats: UserStats) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                value = "${stats.steps}",
                label = "Steps",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(36.dp, 16.dp, 28.dp, 28.dp)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFireDepartment,
                value = "${stats.calories}",
                label = "Calories",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp, 36.dp, 28.dp, 28.dp)
            )
        }
        StatCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Star,
            value = "${stats.xp}",
            label = "Total XP Earned",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(28.dp, 28.dp, 16.dp, 36.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Cake,
                value = if (stats.age > 0) "${stats.age}" else "--",
                label = "Age",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(28.dp, 28.dp, 16.dp, 36.dp)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Height,
                value = if (stats.height > 0) "${stats.height}cm" else "--",
                label = "Height",
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp, 28.dp, 36.dp, 16.dp)
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    containerColor: Color,
    shape: Shape = MaterialExpressiveShapes.asymmetric1
) {
    val contentColor = contentColorFor(containerColor)
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "StatCardScale"
    )

    ElevatedCard(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    }
                )
            },
        shape = shape,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                color = contentColor.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SyncStatusCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun AvatarSelectionDialog(onDismiss: () -> Unit, onSelect: (AvatarType) -> Unit) {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        title = { Text("Choose Your Hero", fontWeight = FontWeight.ExtraBold) },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AvatarOption(
                    type = AvatarType.MALE, 
                    imageRes = com.bushy.health.R.drawable.male_neutral,
                    imageLoader = imageLoader,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f)
                )
                AvatarOption(
                    type = AvatarType.FEMALE, 
                    imageRes = com.bushy.health.R.drawable.female_neutral,
                    imageLoader = imageLoader,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AvatarOption(
    type: AvatarType, 
    imageRes: Int,
    imageLoader: ImageLoader,
    onSelect: (AvatarType) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = { onSelect(type) },
        modifier = modifier.aspectRatio(0.8f),
        shape = MaterialExpressiveShapes.cookie4
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageRes)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                type.name, 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ChangelogDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Celebration, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary) },
        title = { 
            Text(
                "What's New in Bushy", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChangelogItem(
                    title = "Interactive Vector Avatar",
                    description = "Bushy is now a 60/120 FPS procedural avatar! Its eyes follow your touch anywhere on the screen in real-time."
                )
                ChangelogItem(
                    title = "Material 3 Expressive Shapes",
                    description = "Unique shapes for every card! Enjoy 4 & 9-sided cookies, 4-leaf clovers, starbursts, and arches."
                )
                ChangelogItem(
                    title = "Smart Sync & Notifications",
                    description = "Sync is now in the top-left menu with a red-dot alert when activity data needs syncing."
                )
                ChangelogItem(
                    title = "Bushy Wushy AI & Mono Dark",
                    description = "Enhanced AI companion with 30-key rotation, thinking animations, and full Mono Dark theme support."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Let's Go!")
            }
        },
        shape = RoundedCornerShape(32.dp)
    )
}

@Composable
fun ChangelogItem(title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = 8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(
                description, 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
