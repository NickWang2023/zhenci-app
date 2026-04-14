package com.zhenci.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.zhenci.app.ui.screens.MainScreen
import com.zhenci.app.ui.screens.SplashScreen
import com.zhenci.app.ui.theme.ZhenciTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZhenciTheme {
                var showSplash by remember { mutableStateOf(true) }
                
                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    MainScreen()
                }
            }
        }
    }
}