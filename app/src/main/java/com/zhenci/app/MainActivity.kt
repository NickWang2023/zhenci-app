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
    
    private var refreshTrigger by mutableIntStateOf(0)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZhenciTheme {
                var showSplash by remember { mutableStateOf(true) }
                
                // 使用 refreshTrigger 强制刷新
                val currentRefresh = remember { refreshTrigger }
                
                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    MainScreen(refreshTrigger = currentRefresh)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 从其他 Activity 返回时触发刷新
        refreshTrigger++
        android.util.Log.d("MainActivity", "onResume: 触发刷新 refreshTrigger=$refreshTrigger")
    }
}