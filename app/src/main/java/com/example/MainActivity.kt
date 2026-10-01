package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.Collider3DCanvas
import com.example.ui.ColliderViewModel
import com.example.ui.DashboardPanel
import com.example.ui.theme.ParticleColliderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: ColliderViewModel = viewModel()
            val simState by viewModel.state.collectAsState()

            ParticleColliderTheme(darkTheme = simState.isDarkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        // 60% Interactive 3D Collider Environment Viewport
                        Collider3DCanvas(
                            viewModel = viewModel,
                            modifier = Modifier
                                .weight(0.60f)
                                .fillMaxHeight(),
                            onExitClick = {
                                moveTaskToBack(true)
                                finish()
                            }
                        )

                        // 40% Telemetry & Control Dashboard Viewport
                        DashboardPanel(
                            viewModel = viewModel,
                            modifier = Modifier
                                .weight(0.40f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}
