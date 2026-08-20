package com.bushy.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.health.connect.client.PermissionController
import com.bushy.health.ui.GameScreen
import com.bushy.health.ui.theme.BushyTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val healthManager = HealthManager(this)
        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
        val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(healthManager.permissions)) {
                viewModel.refreshStats()
            }
        }

        setContent {
            BushyTheme {
                LaunchedEffect(Unit) {
                    if (!healthManager.hasPermissions()) {
                        requestPermissions.launch(healthManager.permissions)
                    } else {
                        viewModel.refreshStats()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GameScreen(viewModel = viewModel)
                }
            }
        }
    }
}
