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
                // 使用 remember 来跟踪刷新状态，当 Activity 恢复时会重新组合
                var refreshTrigger by remember { mutableIntStateOf(0) }
                
                // 监听生命周期恢复事件
                DisposableEffect(Unit) {
                    val lifecycleObserver = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            refreshTrigger++
                            android.util.Log.d("MainActivity", "onResume: 触发刷新 refreshTrigger=$refreshTrigger")
                        }
                    }
                    lifecycle.addObserver(lifecycleObserver)
                    onDispose {
                        lifecycle.removeObserver(lifecycleObserver)
                    }
                }
                
                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    MainScreen(refreshTrigger = refreshTrigger)
                }
            }
        }
    }
}