package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainAppScreen
import com.example.ui.VitalsViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val vitalsViewModel: VitalsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by vitalsViewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = uiState.themeMode) {
                MainAppScreen(viewModel = vitalsViewModel)
            }
        }
    }
}
