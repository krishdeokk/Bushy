package com.bushy.health

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val healthManager = HealthManager(application)
    private val prefs = application.getSharedPreferences("bushy_prefs", Context.MODE_PRIVATE)
    private var expressionJob: Job? = null
    
    private var tapCount = 0
    private var lastTapTime = 0L

    private val _uiState = MutableStateFlow(loadStats())
    val uiState: StateFlow<UserStats> = _uiState.asStateFlow()

    init {
        refreshStats()
    }

    private fun loadStats(): UserStats {
        return UserStats(
            userName = prefs.getString("user_name", "") ?: "",
            age = prefs.getInt("age", 0),
            height = prefs.getInt("height", 0),
            isSetupComplete = prefs.getBoolean("is_setup_complete", false),
            themeMode = ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name),
            visualStyle = VisualStyle.valueOf(prefs.getString("visual_style", VisualStyle.MATERIAL3.name) ?: VisualStyle.MATERIAL3.name),
            xp = prefs.getLong("xp", 0L),
            avatarType = AvatarType.valueOf(prefs.getString("avatar_type", AvatarType.MALE.name) ?: AvatarType.MALE.name),
            tasks = emptyList() // Tasks could be persisted too, but keeping it simple for now
        )
    }

    private fun saveStats(state: UserStats) {
        prefs.edit().apply {
            putString("user_name", state.userName)
            putInt("age", state.age)
            putInt("height", state.height)
            putBoolean("is_setup_complete", state.isSetupComplete)
            putString("theme_mode", state.themeMode.name)
            putString("visual_style", state.visualStyle.name)
            putLong("xp", state.xp)
            putString("avatar_type", state.avatarType.name)
            apply()
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            val status = healthManager.getSdkStatus()
            if (status != androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
                _uiState.update { it.copy(syncMessage = "Health Connect not available (Status: $status)") }
                return@launch
            }

            if (!healthManager.hasPermissions()) {
                _uiState.update { it.copy(syncMessage = "Waiting for permissions...") }
                return@launch
            }

            val syncedSteps = healthManager.readDailySteps()
            _uiState.update { state -> 
                val finalSteps = if (syncedSteps > state.steps) syncedSteps else state.steps
                
                val updatedTasks = state.tasks.map { task ->
                    when (task.type) {
                        TaskType.STEPS -> {
                            val taskSteps = if (syncedSteps > task.current) syncedSteps.toInt() else task.current
                            task.copy(current = taskSteps, isCompleted = taskSteps >= task.target)
                        }
                        else -> task
                    }
                }
                
                val expression = if (syncedSteps > 0 && state.steps == 0L) AvatarExpression.HAPPY else state.expression

                state.copy(
                    steps = finalSteps,
                    xp = state.xp + (if (finalSteps > state.steps) (finalSteps - state.steps) / 10 else 0),
                    tasks = updatedTasks,
                    expression = expression,
                    syncMessage = if (syncedSteps > 0) null else "Google Health has 0 steps. Tip: Open Google Fit Settings and tap 'Sync now'."
                ).also { saveStats(it) }
            }
            
            if (_uiState.value.expression == AvatarExpression.HAPPY) {
                setExpression(AvatarExpression.HAPPY, 3000)
            }
        }
    }

    private fun setExpression(expression: AvatarExpression, durationMillis: Long = 2000) {
        expressionJob?.cancel()
        expressionJob = viewModelScope.launch {
            _uiState.update { it.copy(expression = expression) }
            delay(durationMillis)
            _uiState.update { it.copy(expression = AvatarExpression.NEUTRAL) }
        }
    }

    fun incrementTask(taskId: String) {
        _uiState.update { state ->
            val taskToIncrement = state.tasks.find { it.id == taskId } ?: return@update state
            if (taskToIncrement.isCompleted) return@update state

            val isStepTask = taskToIncrement.type == TaskType.STEPS
            val isManualTask = taskToIncrement.type == TaskType.PUSHUPS
            val incrementAmount = if (isStepTask) 100 else 1

            val newCurrent = (taskToIncrement.current + incrementAmount).coerceAtMost(taskToIncrement.target)
            val newlyCompleted = newCurrent >= taskToIncrement.target

            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    task.copy(current = newCurrent, isCompleted = newlyCompleted)
                } else task
            }

            // XP Rewards: 10 per manual, 1 per 10 steps, 500 for mission complete
            val earnedXp = (if (isManualTask) 10 else if (isStepTask) 10 else 0) + 
                          (if (newlyCompleted) 500 else 0)

            val oldLevel = state.level
            var expression = if (newlyCompleted) AvatarExpression.CELEBRATING else AvatarExpression.WORKING_OUT
            
            val xpAdded = state.xp + earnedXp
            val newLevel = (Math.sqrt(xpAdded.toDouble() / 100.0).toInt() + 1)

            if (newLevel > oldLevel) {
                expression = AvatarExpression.CELEBRATING
            }

            state.copy(
                tasks = updatedTasks,
                xp = xpAdded,
                pushups = if (isManualTask) state.pushups + incrementAmount else state.pushups,
                steps = if (isStepTask) state.steps + incrementAmount else state.steps,
                expression = expression
            ).also { saveStats(it) }
        }
        
        // Use setExpression to revert to IDLE after a delay
        val currentState = _uiState.value
        if (currentState.expression != AvatarExpression.NEUTRAL) {
            setExpression(currentState.expression, if (currentState.expression == AvatarExpression.CELEBRATING) 4000 else 1000)
        }
    }

    fun resetTask(taskId: String) {
        _uiState.update { state ->
            val taskToReset = state.tasks.find { it.id == taskId } ?: return@update state
            val updatedTasks = state.tasks.map { task ->
                if (task.id == taskId) {
                    task.copy(current = 0, isCompleted = false)
                } else task
            }
            
            val isStepTask = taskToReset.type == TaskType.STEPS
            val isManualTask = taskToReset.type == TaskType.PUSHUPS
            
            state.copy(
                tasks = updatedTasks,
                steps = if (isStepTask) (state.steps - taskToReset.current).coerceAtLeast(0) else state.steps,
                pushups = if (isManualTask) (state.pushups - taskToReset.current).coerceAtLeast(0) else state.pushups,
                expression = AvatarExpression.TIRED
            ).also { saveStats(it) }
        }
        setExpression(AvatarExpression.TIRED, 2000)
    }

    fun resetAllStats() {
        _uiState.update { state ->
            state.copy(
                steps = 0,
                pushups = 0,
                xp = 0,
                tasks = emptyList(),
                expression = AvatarExpression.TIRED
            ).also { saveStats(it) }
        }
        setExpression(AvatarExpression.TIRED, 3000)
    }

    fun addNewTask(title: String, target: Int, type: TaskType) {
        _uiState.update { state ->
            val newTask = HealthTask(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                target = target,
                current = 0,
                type = type
            )
            state.copy(tasks = state.tasks + newTask, expression = AvatarExpression.EXCITED)
        }
        setExpression(AvatarExpression.EXCITED, 2000)
    }

    fun setAvatar(type: AvatarType) {
        _uiState.update { 
            it.copy(avatarType = type, expression = AvatarExpression.HAPPY).also { state -> saveStats(state) } 
        }
        setExpression(AvatarExpression.HAPPY, 2000)
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { 
            it.copy(themeMode = mode).also { state -> saveStats(state) }
        }
    }

    fun setVisualStyle(style: VisualStyle) {
        _uiState.update { 
            it.copy(visualStyle = style).also { state -> saveStats(state) }
        }
    }

    fun updateProfile(name: String, age: Int, height: Int) {
        _uiState.update { 
            it.copy(
                userName = name,
                age = age,
                height = height,
                expression = AvatarExpression.HAPPY
            ).also { state -> saveStats(state) } 
        }
        setExpression(AvatarExpression.HAPPY, 2000)
    }

    fun completeSetup() {
        _uiState.update { 
            it.copy(isSetupComplete = true, expression = AvatarExpression.CELEBRATING).also { state -> saveStats(state) } 
        }
        setExpression(AvatarExpression.CELEBRATING, 3000)
    }

    fun onScreenTouch() {
        // Only trigger attentive if we're in neutral or something minor
        if (_uiState.value.expression == AvatarExpression.NEUTRAL) {
            setExpression(AvatarExpression.ATTENTIVE, 2000)
        }
    }

    fun onAvatarTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < 2000) {
            tapCount++
        } else {
            tapCount = 1
        }
        lastTapTime = now

        if (tapCount >= 5) {
            setExpression(AvatarExpression.ANGRY, 3000)
            tapCount = 0
            return
        }

        expressionJob?.cancel()
        expressionJob = viewModelScope.launch {
            _uiState.update { it.copy(expression = AvatarExpression.SURPRISED) }
            delay(2000)
            _uiState.update { it.copy(expression = AvatarExpression.EXCITED) }
            delay(2500)
            _uiState.update { it.copy(expression = AvatarExpression.HAPPY) }
            delay(5000)
            _uiState.update { it.copy(expression = AvatarExpression.NEUTRAL) }
        }
    }

    fun onAvatarDoubleTap() {
        if (_uiState.value.expression == AvatarExpression.HAPPY) {
            setExpression(AvatarExpression.LAUGHING, 3000)
        }
    }
}
