package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainAppRoot
import com.example.ui.theme.AbsensiPjokTheme
import com.example.ui.viewmodel.PjokViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(9, 27, 51))
        )
        setContent {
            AbsensiPjokTheme {
                val pjokViewModel: PjokViewModel = viewModel()
                MainAppRoot(viewModel = pjokViewModel)
            }
        }
    }
}
