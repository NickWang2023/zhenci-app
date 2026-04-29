package com.zhenci.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zhenci.app.data.entity.Template

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    object Today : Screen("today", "今日", Icons.Default.CalendarToday)
    object Templates : Screen("templates", "模板", Icons.Default.Description)
    object Stats : Screen("stats", "积分", Icons.Default.EmojiEvents)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
    object TemplateDetail : Screen("template_detail/{templateId}", "模板详情", null)

    companion object {
        fun templateDetailRoute(templateId: Long) = "template_detail/$templateId"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Today, Screen.Templates, Screen.Stats, Screen.Settings)

    // 监听当前路由，判断是否显示底部导航栏
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && 
                        !currentRoute.startsWith("template_detail")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) { TodayScreen() }
            composable(Screen.Templates.route) { 
                TemplatesScreen(
                    onTemplateClick = { template ->
                        navController.navigate(Screen.templateDetailRoute(template.id))
                    }
                ) 
            }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(
                route = Screen.TemplateDetail.route,
                arguments = listOf(navArgument("templateId") { type = NavType.LongType })
            ) { backStackEntry ->
                val templateId = backStackEntry.arguments?.getLong("templateId") ?: 0L
                // 这里需要从数据库获取模板信息
                TemplateDetailScreenWrapper(
                    templateId = templateId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
