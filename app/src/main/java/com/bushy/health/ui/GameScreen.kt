package com.bushy.health.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bushy.health.AvatarType
import com.bushy.health.GameViewModel
import com.bushy.health.UserStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showAvatarSelection by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bushy Health Game") },
                actions = {
                    IconButton(onClick = { com.bushy.health.ShareUtils.shareProgress(context, uiState) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                AvatarDisplay(
                    type = uiState.avatarType,
                    level = uiState.level,
                    onClick = { showAvatarSelection = true }
                )
            }

            item {
                StatsCard(uiState)
            }

            item {
                TaskSection(
                    onAddPushup = { viewModel.addPushups(5) },
                    onRefresh = { viewModel.refreshStats() }
                )
            }
        }
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
}

@Composable
fun AvatarDisplay(type: AvatarType, level: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (type == AvatarType.MALE) Icons.Default.Male else Icons.Default.Female,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = "Level $level",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        TextButton(onClick = onClick) {
            Text("Change Avatar")
        }
    }
}

@Composable
fun StatsCard(stats: UserStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Progress", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { stats.xp.toFloat() / stats.nextLevelXp.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Text(
                text = "${stats.xp} / ${stats.nextLevelXp} XP to Level ${stats.level + 1}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(Icons.AutoMirrored.Filled.DirectionsWalk, "${stats.steps}", "Steps")
                StatItem(Icons.Default.FitnessCenter, "${stats.pushups}", "Pushups")
            }
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun TaskSection(onAddPushup: () -> Unit, onRefresh: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Daily Tasks", style = MaterialTheme.typography.titleMedium)
        
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sync Steps")
        }

        Button(
            onClick = onAddPushup,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Record 5 Pushups")
        }
    }
}

@Composable
fun AvatarSelectionDialog(onDismiss: () -> Unit, onSelect: (AvatarType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Avatar") },
        text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AvatarOption(AvatarType.MALE, Icons.Default.Male, onSelect)
                AvatarOption(AvatarType.FEMALE, Icons.Default.Female, onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AvatarOption(type: AvatarType, icon: ImageVector, onSelect: (AvatarType) -> Unit) {
    IconButton(
        onClick = { onSelect(type) },
        modifier = Modifier.size(80.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp))
            Text(type.name, style = MaterialTheme.typography.bodySmall)
        }
    }
}
