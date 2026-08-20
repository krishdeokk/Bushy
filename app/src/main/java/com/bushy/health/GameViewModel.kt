package com.bushy.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val healthManager = HealthManager(application)

    private val _uiState = MutableStateFlow(UserStats())
    val uiState: StateFlow<UserStats> = _uiState.asStateFlow()

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            val steps = healthManager.readDailySteps()
            _uiState.update { it.copy(steps = steps) }
            calculateXp()
        }
    }

    fun addPushups(count: Int) {
        _uiState.update { it.copy(pushups = it.pushups + count) }
        calculateXp()
    }

    fun setAvatar(type: AvatarType) {
        _uiState.update { it.copy(avatarType = type) }
    }

    private fun calculateXp() {
        val stepsXp = (_uiState.value.steps / 100).toInt() // 1 XP per 100 steps
        val pushupsXp = _uiState.value.pushups * 5 // 5 XP per pushup
        val totalXp = stepsXp + pushupsXp

        var currentLevel = 1
        var remainingXp = totalXp
        
        while (remainingXp >= currentLevel * 100) {
            remainingXp -= currentLevel * 100
            currentLevel++
        }

        _uiState.update { it.copy(level = currentLevel, xp = remainingXp) }
    }
}
