package com.zhenci.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// 主色 - Primary Colors
// ============================================

/** 深蓝 - 主色 */
val DeepBlue = Color(0xFF1976D2)
/** 亮蓝 - 辅助主色/悬浮状态 */
val BrightBlue = Color(0xFF2196F3)
/** 专业绿 - 成功/专业 */
val ProfessionalGreen = Color(0xFF4CAF50)
/** 玫瑰灰/中性强提醒 - 次要用于提醒按钮或标识 */
val WarningOrange = Color(0xFFFF9800)

// ============================================
// 背景色/中性色 - Background & Neutral Colors
// ============================================

/** 白色 - 背景 */
val White = Color(0xFFFFFFFF)
/** 亮灰 - 页面底色 */
val LightGray = Color(0xFFF7F8FA)
/** 浅灰 - 分割线/容器 */
val PaleGray = Color(0xFFE5E7EB)
/** 深灰 - 文字/细分区 */
val DarkGray = Color(0xFF374151)
/** 灰色 - 辅助说明文字 */
val MediumGray = Color(0xFF757575)

// ============================================
// 高亮/交互色 - Accent & Interactive Colors
// ============================================

/** 品牌蓝 - 按钮、选中 */
val BrandBlue = Color(0xFF2563EB)
/** 冷静绿 - 完成/成功 */
val CalmGreen = Color(0xFF2ECC71)

// ============================================
// Material Theme 适配色 - Material Theme Colors
// ============================================

/** 主题主色 */
val ZhenciPrimary = DeepBlue
/** 主题次色 */
val ZhenciSecondary = BrightBlue
/** 主题背景 */
val ZhenciBackground = LightGray
/** 主题表面 */
val ZhenciSurface = White
/** 错误色 */
val ZhenciError = Color(0xFFDC2626)
/** 成功色 */
val ZhenciSuccess = ProfessionalGreen
/** 警告色 */
val ZhenciWarning = WarningOrange

// ============================================
// 任务类型色 - Task Type Colors
// ============================================

/** 工作类任务 */
val WorkTaskColor = DeepBlue
/** 生活类任务 */
val LifeTaskColor = ProfessionalGreen
/** 其他类任务 */
val OtherTaskColor = MediumGray

// ============================================
// 旧配色兼容 - Legacy Colors (保留用于兼容)
// ============================================

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
