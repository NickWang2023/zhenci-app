# 针刺 - 自律提醒应用

一款帮助用户严格管理每日工作与生活节奏的 Android 应用。

## 功能特性

- ⏰ 24小时定时提醒 - 精确到分钟的日程安排
- 🔊 语音播报提醒 - 到达时间点自动语音播报
- 📋 每日模板管理 - 新建、编辑、导入模板
- 🎯 自律监督 - 强制提醒，不错过任何重要事项

## 技术栈

- Kotlin
- Jetpack Compose
- Room Database
- AlarmManager + WorkManager
- Text-to-Speech (TTS)

## 项目结构

```
app/
├── src/main/java/com/zhenci/app/
│   ├── MainActivity.kt
│   ├── ZhenciApplication.kt
│   ├── data/
│   │   ├── database/
│   │   ├── entity/
│   │   └── repository/
│   ├── ui/
│   │   ├── screens/
│   │   ├── components/
│   │   └── theme/
│   ├── service/
│   │   └── AlarmService.kt
│   └── utils/
└── src/main/res/
```

## 构建说明

1. 使用 Android Studio 打开项目
2. 同步 Gradle
3. 构建 APK：Build → Build Bundle(s) / APK(s) → Build APK(s)

## 使用指南

1. 首次打开创建每日模板
2. 添加任务事项和提醒时间
3. 保存并启用提醒
4. 到达时间点自动语音播报
