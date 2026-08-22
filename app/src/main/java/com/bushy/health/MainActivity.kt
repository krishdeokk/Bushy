package com.bushy.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.health.connect.client.PermissionController
import com.bushy.health.ui.GameScreen
import com.bushy.health.ui.theme.BushyTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val healthManager = HealthManager(this)
        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
        val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(healthManager.permissions)) {
                viewModel.refreshStats()
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            
            BushyTheme(
                avatarType = uiState.avatarType, 
                themeMode = uiState.themeMode,
                visualStyle = uiState.visualStyle
            ) {
                LaunchedEffect(Unit) {
                    if (healthManager.hasPermissions()) {
                        viewModel.refreshStats()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GameScreen(
                        viewModel = viewModel,
                        onRequestPermissions = { requestPermissions.launch(healthManager.permissions) }
                    )
                }
            }
        }
    }
}
