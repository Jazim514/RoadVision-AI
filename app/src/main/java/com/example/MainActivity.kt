package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.MainScreen
import com.example.ui.RoadVisionViewModel
import com.example.ui.theme.RoadDeepNavy
import com.example.ui.theme.RoadVisionTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RoadVisionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoadVisionTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RoadDeepNavy
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
