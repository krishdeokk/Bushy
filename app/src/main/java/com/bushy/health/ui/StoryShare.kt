package com.bushy.health.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bushy.health.*

@Composable
fun StoryShareCard(
    stats: UserStats,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val gifName = remember(stats.avatarType, stats.expression, stats.visualStyle) {
        val expr = stats.expression.name.lowercase()
        if (stats.visualStyle == VisualStyle.MONOCHROME) {
            "mono_${expr}"
        } else {
            val gender = stats.avatarType.name.lowercase()
            "${gender}_${expr}"
        }
    }
    
    val gifResId = remember(gifName) {
        val id = context.resources.getIdentifier(gifName, "drawable", context.packageName)
        if (id != 0) id else null
    }

    // 9:16 Aspect Ratio container
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Gradient background decoration
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            // Background patterns (abstract circles)
            Box(
                modifier = Modifier
                    .offset(x = (-50).dp, y = (-50).dp)
                    .size(250.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = 80.dp)
                    .size(300.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // User Badge & Progress
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "LEVEL ${stats.level}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${stats.xpInCurrentLevel} / ${stats.xpRequiredForNextLevel} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Header
                Text(
                    text = if (stats.userName.isNotBlank()) "${stats.userName}'s Journey" else "My Bushy Journey",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sleeker Progress Bar
                LinearProgressIndicator(
                    progress = { stats.levelProgress },
                    modifier = Modifier
                        .width(140.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                Spacer(modifier = Modifier.weight(1f))

                // Large Avatar
                val isDark = when (stats.themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
                
                val circleColor = when {
                    stats.visualStyle == VisualStyle.MONOCHROME && isDark -> Color.White
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                }

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(
                            circleColor,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (gifResId != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(gifResId)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.9f),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1.2f))

                // Stats Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StoryStatItem(
                            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                            value = "${stats.steps}",
                            label = "Steps",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        StoryStatItem(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "${stats.calories}",
                            label = "Calories",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    StoryStatItem(
                        icon = Icons.Default.Star,
                        value = "${stats.xp} XP Total",
                        label = "Overall Progress",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.weight(0.8f))

                // Branding
                Text(
                    "B U S H Y",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun StoryStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val contentColor = if (containerColor == MaterialTheme.colorScheme.background) {
        MaterialTheme.colorScheme.onBackground
    } else {
        contentColorFor(containerColor)
    }
    
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, 
                null, 
                modifier = Modifier.size(24.dp), 
                tint = contentColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
