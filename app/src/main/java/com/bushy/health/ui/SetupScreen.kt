package com.bushy.health.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bushy.health.GameViewModel
import com.bushy.health.ThemeMode
import com.bushy.health.VisualStyle
import com.bushy.health.AvatarType
import com.bushy.health.R
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    viewModel: GameViewModel,
    onRequestPermissions: () -> Unit
) {
    val pagerState = rememberPagerState { 6 }
    val scope = rememberCoroutineScope()
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
    
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(AvatarType.MALE) }
    var selectedVisualStyle by remember { mutableStateOf(VisualStyle.MATERIAL3) }
    var selectedThemeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(64.dp))
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> PermissionStep(onRequestPermissions)
                    1 -> GenderStep(selectedGender, imageLoader) { 
                        selectedGender = it
                        viewModel.setAvatar(it) 
                    }
                    2 -> VibeStep(selectedVisualStyle, imageLoader) {
                        selectedVisualStyle = it
                        viewModel.setVisualStyle(it)
                    }
                    3 -> {
                        ThemeStep(selectedThemeMode) {
                            selectedThemeMode = it
                            viewModel.setThemeMode(it)
                        }
                    }
                    4 -> NameStep(name) { name = it }
                    5 -> StatsStep(age, height, { age = it }, { height = it })
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedIconButton(
                        onClick = {
                            scope.launch {
                                if (pagerState.currentPage == 4 && selectedVisualStyle == VisualStyle.MONOCHROME) {
                                    pagerState.animateScrollToPage(2)
                                } else {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage < 5) {
                                if (pagerState.currentPage == 2 && selectedVisualStyle == VisualStyle.MONOCHROME) {
                                    pagerState.animateScrollToPage(4)
                                } else {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                viewModel.updateProfile(name, age.toIntOrNull() ?: 0, height.toIntOrNull() ?: 0)
                                viewModel.completeSetup()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    enabled = when(pagerState.currentPage) {
                        4 -> name.isNotBlank()
                        5 -> age.isNotBlank() && height.isNotBlank()
                        else -> true
                    }
                ) {
                    Text(
                        if (pagerState.currentPage == 5) "Begin Adventure" else "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(if (pagerState.currentPage == 5) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
fun PermissionStep(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Welcome to Bushy",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 44.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "To track your heroic progress, we need to sync with Google Health Connect.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        FilledTonalButton(
            onClick = onRequestPermissions,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Grant Health Access")
        }
    }
}

@Composable
fun GenderStep(selected: AvatarType, imageLoader: ImageLoader, onSelect: (AvatarType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Choose Your Hero",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            GenderCard(
                imageRes = R.drawable.male_neutral,
                imageLoader = imageLoader,
                label = "Male",
                selected = selected == AvatarType.MALE,
                onClick = { onSelect(AvatarType.MALE) },
                modifier = Modifier.weight(1f)
            )
            GenderCard(
                imageRes = R.drawable.female_neutral,
                imageLoader = imageLoader,
                label = "Female",
                selected = selected == AvatarType.FEMALE,
                onClick = { onSelect(AvatarType.FEMALE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun GenderCard(
    imageRes: Int,
    imageLoader: ImageLoader,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.aspectRatio(0.8f),
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(
            4.dp, 
            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
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
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VibeStep(selected: VisualStyle, imageLoader: ImageLoader, onSelect: (VisualStyle) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Choose Your Vibe",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            VibeCard(
                imageRes = R.drawable.male_neutral,
                imageLoader = imageLoader,
                label = "Vibrant",
                subLabel = "Material 3",
                selected = selected == VisualStyle.MATERIAL3,
                onClick = { onSelect(VisualStyle.MATERIAL3) },
                modifier = Modifier.weight(1f)
            )
            VibeCard(
                imageRes = R.drawable.mono_neutral,
                imageLoader = imageLoader,
                label = "Minimal",
                subLabel = "Monochrome",
                selected = selected == VisualStyle.MONOCHROME,
                onClick = { onSelect(VisualStyle.MONOCHROME) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun VibeCard(
    imageRes: Int,
    imageLoader: ImageLoader,
    label: String,
    subLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.aspectRatio(0.8f),
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(
            4.dp, 
            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
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
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ThemeStep(mode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Visual Style",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThemeCard(
                icon = Icons.Default.LightMode,
                label = "Light Mode",
                selected = mode == ThemeMode.LIGHT,
                onClick = { onSelect(ThemeMode.LIGHT) },
                modifier = Modifier.fillMaxWidth()
            )
            ThemeCard(
                icon = Icons.Default.DarkMode,
                label = "Dark Mode",
                selected = mode == ThemeMode.DARK,
                onClick = { onSelect(ThemeMode.DARK) },
                modifier = Modifier.fillMaxWidth()
            )
            ThemeCard(
                icon = Icons.Default.SettingsSuggest,
                label = "Follow System",
                selected = mode == ThemeMode.SYSTEM,
                onClick = { onSelect(ThemeMode.SYSTEM) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ThemeCard(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            2.dp, 
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun NameStep(name: String, onNameChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "What should Bushy call you?",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            lineHeight = 40.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Your Hero Name", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun StatsStep(age: String, height: String, onAgeChange: (String) -> Unit, onHeightChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Final details...",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = age,
                onValueChange = { if (it.all { c -> c.isDigit() }) onAgeChange(it) },
                label = { Text("Age") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = height,
                onValueChange = { if (it.all { c -> c.isDigit() }) onHeightChange(it) },
                label = { Text("Height (cm)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "These stats help Bushy calculate your calorie burn more accurately.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
