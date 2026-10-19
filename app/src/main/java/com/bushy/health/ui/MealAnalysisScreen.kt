package com.bushy.health.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bushy.health.EditableFoodItem
import com.bushy.health.IndianNutritionDatabase
import com.bushy.health.IndianNutritionEntry
import com.bushy.health.MealAnalysisResult
import com.bushy.health.MultilingualDishInterpreter
import com.bushy.health.PortionUnit
import com.bushy.health.PrepStyle
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealAnalysisScreen(
    analysisResult: MealAnalysisResult,
    mealBitmap: Bitmap?,
    onBack: () -> Unit,
    onRetakePhoto: () -> Unit,
    onSaveMeal: (MealAnalysisResult) -> Unit
) {
    var currentItems by remember { mutableStateOf(analysisResult.items) }
    var originalItems by remember { mutableStateOf(analysisResult.items) }
    var itemToEditIndex by remember { mutableStateOf<Int?>(null) }
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var showCustomFoodDialog by remember { mutableStateOf(false) }
    var showAiModelInfoDialog by remember { mutableStateOf(false) }

    val updatedResult = remember(currentItems) {
        analysisResult.copy(items = currentItems)
    }

    val totalNutrients = updatedResult.totalNutrients
    val calorieRange = updatedResult.calorieRange

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Indian Meal Calorie Estimator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAiModelInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "AI Pipeline Info", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { onSaveMeal(updatedResult) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Confirmed Meal (${totalNutrients.calories} kcal)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Image Preview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        if (mealBitmap != null) {
                            Image(
                                bitmap = mealBitmap.asImageBitmap(),
                                contentDescription = "Meal Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = analysisResult.mealName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showAiModelInfoDialog = true }
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = analysisResult.analysisMethod.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onRetakePhoto,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Retake")
                            }
                        }
                    }
                }
            }

            // Non-Food Detection Warning Card
            if (!analysisResult.isFoodDetected || analysisResult.detectionWarning != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "No Food Item Detected",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = analysisResult.detectionWarning ?: "The photo appears to be a keyboard or non-food object. Please take a photo of an Indian meal plate, or add food items manually below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onRetakePhoto,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Retake Photo")
                                }

                                OutlinedButton(
                                    onClick = { showAddFoodDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Manually")
                                }
                            }
                        }
                    }
                }
            }

            // Calorie Range & Macro Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Estimated Calories", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "${totalNutrients.calories} kcal",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = "Range: ${calorieRange.minCalories} – ${calorieRange.maxCalories} kcal",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Macro Badges Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MacroChip("Protein", "${totalNutrients.proteinG}g", Color(0xFF4CAF50))
                            MacroChip("Carbs", "${totalNutrients.carbsG}g", Color(0xFF2196F3))
                            MacroChip("Fat", "${totalNutrients.fatG}g", Color(0xFFFF9800))
                        }

                        Spacer(Modifier.height(12.dp))

                        // Assumption & Uncertainty Explanation
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = calorieRange.assumptionsNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Section Title & Add Food Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Detected Items (${currentItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = { showAddFoodDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Dish")
                    }
                }
            }

            // Editable Food Items List
            if (currentItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddFoodDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No Food Items Added Yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap here to search and select Indian dishes manually (supports English & Indic scripts).",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(currentItems) { index, item ->
                    FoodItemCard(
                        item = item,
                        onEditName = { itemToEditIndex = index },
                        onUpdateItem = { updatedItem ->
                            currentItems = currentItems.toMutableList().apply {
                                set(index, updatedItem)
                            }
                        },
                        onRemove = {
                            currentItems = currentItems.toMutableList().apply {
                                removeAt(index)
                            }
                        }
                    )
                }
            }

            // Medical Disclaimer Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Disclaimer: Photo calorie estimates are for personal fitness tracking only and do not constitute clinical dietary advice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (itemToEditIndex != null && itemToEditIndex!! in currentItems.indices) {
        val index = itemToEditIndex!!
        val originalItem = if (index in originalItems.indices) originalItems[index] else currentItems[index]

        EditDishNameDialog(
            item = currentItems[index],
            onDismiss = { itemToEditIndex = null },
            onConfirmCorrection = { correctedItem ->
                currentItems = currentItems.toMutableList().apply {
                    set(index, correctedItem)
                }
                itemToEditIndex = null
            },
            onRevertToOriginal = {
                currentItems = currentItems.toMutableList().apply {
                    set(index, originalItem)
                }
                itemToEditIndex = null
            }
        )
    }

    if (showAddFoodDialog) {
        AddFoodDialog(
            onDismiss = { showAddFoodDialog = false },
            onCreateCustomClicked = {
                showAddFoodDialog = false
                showCustomFoodDialog = true
            },
            onFoodSelected = { newEntry ->
                val newItem = IndianNutritionDatabase.createEditableFoodItem(newEntry)
                currentItems = currentItems + newItem
                showAddFoodDialog = false
            }
        )
    }

    if (showCustomFoodDialog) {
        CreateCustomFoodDialog(
            onDismiss = { showCustomFoodDialog = false },
            onFoodCreated = { customEntry ->
                IndianNutritionDatabase.addCustomEntry(customEntry)
                val newItem = IndianNutritionDatabase.createEditableFoodItem(customEntry)
                currentItems = currentItems + newItem
                showCustomFoodDialog = false
            }
        )
    }

    if (showAiModelInfoDialog) {
        AiModelInfoDialog(onDismiss = { showAiModelInfoDialog = false })
    }
}

@Composable
fun FoodItemCard(
    item: EditableFoodItem,
    onEditName: () -> Unit,
    onUpdateItem: (EditableFoodItem) -> Unit,
    onRemove: () -> Unit
) {
    val nutrients = item.calculatedNutrients

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & Edit / Remove Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEditName() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.foodName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Dish Name",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (item.detectedScript != null) {
                        Text(
                            text = "Script: ${item.detectedScript} • Tap name to correct",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        Text(
                            text = "Tap dish name to edit / correct",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Text(
                        text = "${nutrients.calories} kcal • Weight: ${item.totalWeightGrams.toInt()}g",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Macros: P: ${nutrients.proteinG}g | C: ${nutrients.carbsG}g | F: ${nutrients.fatG}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Portion Stepper & Unit Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quantity Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (item.quantity > 0.5f) {
                                onUpdateItem(item.copy(quantity = (item.quantity - 0.5f).coerceAtLeast(0.5f)))
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = if (item.quantity % 1f == 0f) "${item.quantity.toInt()}" else String.format(Locale.US, "%.1f", item.quantity),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            onUpdateItem(item.copy(quantity = item.quantity + 0.5f))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Portion Unit Dropdown
                var unitMenuExpanded by remember { mutableStateOf(false) }

                Box {
                    FilterChip(
                        selected = true,
                        onClick = { unitMenuExpanded = true },
                        label = { Text(item.unit.displayName) }
                    )

                    DropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        PortionUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName) },
                                onClick = {
                                    onUpdateItem(item.copy(unit = unit))
                                    unitMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Prep Style Filter Chips Row
            Text("Preparation Style:", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PrepStyle.entries.forEach { style ->
                    val isSelected = item.prepStyle == style
                    FilterChip(
                        selected = isSelected,
                        onClick = { onUpdateItem(item.copy(prepStyle = style)) },
                        label = { Text(style.displayName, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
fun EditDishNameDialog(
    item: EditableFoodItem,
    onDismiss: () -> Unit,
    onConfirmCorrection: (EditableFoodItem) -> Unit,
    onRevertToOriginal: () -> Unit
) {
    var textInput by remember { mutableStateOf(item.originalUserText ?: item.foodName) }
    val interpretation = remember(textInput) {
        MultilingualDishInterpreter.interpretDishText(textInput)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Correct Dish Name", fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = interpretation.script.languageName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Dish Name (English or Indic Script)") },
                    placeholder = { Text("e.g., paneer, पनींर, ருட்டி, தோசை...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (textInput.isNotEmpty()) {
                            IconButton(onClick = { textInput = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )

                // Typo Suggestion Banner
                if (interpretation.suggestedCorrection != null && interpretation.suggestedCorrection != textInput) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { textInput = interpretation.suggestedCorrection }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Did you mean: ${interpretation.suggestedCorrection}?",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Ambiguous Candidate Selection
                if (interpretation.isAmbiguous && interpretation.candidateMatches.isNotEmpty()) {
                    Text("Select Specific Match:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyColumn(
                        modifier = Modifier.height(140.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(interpretation.candidateMatches) { _, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updatedItem = item.copy(
                                            foodName = entry.name,
                                            caloriesPer100g = entry.caloriesPer100g,
                                            proteinPer100g = entry.proteinPer100g,
                                            carbsPer100g = entry.carbsPer100g,
                                            fatPer100g = entry.fatPer100g,
                                            preparationNote = entry.prepNotes,
                                            originalUserText = textInput,
                                            detectedScript = interpretation.script.displayName
                                        )
                                        onConfirmCorrection(updatedItem)
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(entry.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${entry.regionOrCategory} • ${entry.caloriesPer100g} kcal/100g", style = MaterialTheme.typography.labelSmall)
                                }
                                Icon(Icons.Default.Check, contentDescription = "Select")
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val match = interpretation.bestMatch
                    val updatedItem = if (match != null) {
                        item.copy(
                            foodName = match.name,
                            caloriesPer100g = match.caloriesPer100g,
                            proteinPer100g = match.proteinPer100g,
                            carbsPer100g = match.carbsPer100g,
                            fatPer100g = match.fatPer100g,
                            preparationNote = match.prepNotes,
                            originalUserText = textInput,
                            detectedScript = interpretation.script.displayName
                        )
                    } else {
                        item.copy(
                            foodName = textInput.trim(),
                            originalUserText = textInput,
                            detectedScript = interpretation.script.displayName
                        )
                    }
                    onConfirmCorrection(updatedItem)
                }
            ) {
                Text("Confirm Dish Name")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRevertToOriginal) {
                    Text("Revert", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun MacroChip(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
fun AddFoodDialog(
    onDismiss: () -> Unit,
    onCreateCustomClicked: () -> Unit,
    onFoodSelected: (IndianNutritionEntry) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val interpretation = remember(searchQuery) {
        MultilingualDishInterpreter.interpretDishText(searchQuery)
    }
    val filteredEntries = remember(searchQuery, interpretation) {
        if (searchQuery.isBlank()) {
            IndianNutritionDatabase.items
        } else if (interpretation.candidateMatches.isNotEmpty()) {
            interpretation.candidateMatches
        } else {
            IndianNutritionDatabase.searchEntries(searchQuery)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Indian Food", fontWeight = FontWeight.Bold)
                TextButton(onClick = onCreateCustomClicked) {
                    Text("+ Create Custom")
                }
            }
        },
        text = {
            Column(modifier = Modifier.height(340.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search in English, हिन्दी, தமிழ், বাংলা...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = interpretation.script.languageName,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(filteredEntries) { _, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFoodSelected(entry) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(entry.name, fontWeight = FontWeight.SemiBold)
                                Text("${entry.regionOrCategory} • ${entry.caloriesPer100g} kcal / 100g", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateCustomFoodDialog(
    onDismiss: () -> Unit,
    onFoodCreated: (IndianNutritionEntry) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("200") }
    var proteinText by remember { mutableStateOf("8") }
    var carbsText by remember { mutableStateOf("25") }
    var fatText by remember { mutableStateOf("6") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Indian Food", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food / Dish Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it },
                        label = { Text("Kcal / 100g") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        label = { Text("Carbs (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Fat (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val entry = IndianNutritionEntry(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            aliases = listOf(name.trim().lowercase(Locale.ROOT)),
                            caloriesPer100g = caloriesText.toIntOrNull() ?: 180,
                            proteinPer100g = proteinText.toFloatOrNull() ?: 6f,
                            carbsPer100g = carbsText.toFloatOrNull() ?: 20f,
                            fatPer100g = fatText.toFloatOrNull() ?: 5f,
                            defaultUnit = PortionUnit.KATORI,
                            defaultUnitWeightGrams = 150f,
                            prepNotes = "User custom food entry",
                            regionOrCategory = "Custom Entry",
                            source = "User Entry"
                        )
                        onFoodCreated(entry)
                    }
                }
            ) {
                Text("Add Custom Food")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AiModelInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Local Vision & Multilingual AI Status", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🟢 Active Engines: Local Feature Analyzer & Multilingual Dish Interpreter",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Supports 9 Indian regional languages and scripts (Devanagari, Bengali, Tamil, Telugu, Gujarati, Kannada, Malayalam, Gurmukhi, English) with fuzzy typo auto-correction. All processing happens 100% on-device.",
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider()
                Text(
                    text = "🤖 Local Qwen Vision Model (Qwen2-VL / Qwen3-VL):",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Running a true multi-modal Qwen Vision model on-device requires ~1.8GB 4-bit GGUF binary files, 6GB+ RAM, and native NDK bindings (llama.cpp / Executorch). This ensures full transparency without disguising mocks as AI inference.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}
