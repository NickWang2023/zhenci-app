# 针刺 APP Bug 修复报告

## 测试日期：2026-04-28

---

## 🐛 问题汇总

### 问题1：首页旧任务不提醒，只有新增任务才提醒 ✅ 已修复

**根本原因**：
- 应用启动时（`TaskViewModel.init`）没有重新注册已有任务的闹钟
- 只有调用 `addTask()` 新增任务时才会调用 `alarmScheduler.scheduleTask()`
- 应用重装或系统重启后，AlarmManager 的定时任务会丢失

**修复方案**：
在 `TaskViewModel.init` 中添加 `rescheduleAllAlarms()` 方法，应用启动时重新为所有启用的任务注册闹钟。

**修改文件**：
- `app/src/main/java/com/zhenci/app/viewmodel/TaskViewModel.kt`

---

### 问题2：模板无法编辑任务内容 ✅ 已修复

**根本原因**：
- `TemplatesScreen.kt` 中的创建/编辑对话框**只有「名称」和「描述」两个输入框**
- 完全没有提供编辑模板内任务列表的UI

**修复方案**：
1. 新增 `TemplateDetailScreen.kt` - 模板详情页，可以添加/编辑/删除模板内的任务
2. 新增 `TemplateDetailViewModel.kt` - 管理模板内任务的 ViewModel
3. 新增 `TemplateDetailScreenWrapper.kt` - 导航包装器
4. 修改 `MainScreen.kt` - 添加模板详情页导航路由
5. 修改 `TemplatesScreen.kt` - 点击模板卡片进入详情页

**修改/新增文件**：
- `app/src/main/java/com/zhenci/app/ui/screens/TemplateDetailScreen.kt` (新增)
- `app/src/main/java/com/zhenci/app/viewmodel/TemplateDetailViewModel.kt` (新增)
- `app/src/main/java/com/zhenci/app/ui/screens/TemplateDetailScreenWrapper.kt` (新增)
- `app/src/main/java/com/zhenci/app/ui/screens/MainScreen.kt`
- `app/src/main/java/com/zhenci/app/ui/screens/TemplatesScreen.kt`

---

### 问题3：模板无法应用到首页 ✅ 已修复

**根本原因**：
- `TemplateViewModel.applyTemplate()` 方法从 `template.getTasks()` 获取任务（从 JSON 解析）
- 但模板任务实际存储在 `Task` 表中，通过 `templateId` 字段关联
- 修复问题2后，模板任务通过数据库关联存储，不再依赖 JSON

**修复方案**：
1. 修改 `applyTemplate()` 方法，从数据库查询 `templateId` 关联的任务
2. 添加 `clearExisting` 参数，让用户选择是否清空现有任务
3. 添加应用模板确认对话框 `ApplyTemplateDialog`

**修改文件**：
- `app/src/main/java/com/zhenci/app/viewmodel/TemplateViewModel.kt`
- `app/src/main/java/com/zhenci/app/ui/screens/TemplatesScreen.kt`

---

## 📁 修改文件清单

### 修改的文件
1. `app/src/main/java/com/zhenci/app/viewmodel/TaskViewModel.kt` - 添加闹钟重新注册
2. `app/src/main/java/com/zhenci/app/viewmodel/TemplateViewModel.kt` - 修复应用模板逻辑
3. `app/src/main/java/com/zhenci/app/ui/screens/MainScreen.kt` - 添加模板详情页导航
4. `app/src/main/java/com/zhenci/app/ui/screens/TemplatesScreen.kt` - 添加详情入口和应用确认对话框

### 新增的文件
1. `app/src/main/java/com/zhenci/app/ui/screens/TemplateDetailScreen.kt` - 模板详情页（编辑任务）
2. `app/src/main/java/com/zhenci/app/viewmodel/TemplateDetailViewModel.kt` - 模板详情 ViewModel
3. `app/src/main/java/com/zhenci/app/ui/screens/TemplateDetailScreenWrapper.kt` - 导航包装器

---

## 🎯 新功能流程

### 创建带任务的模板
1. 在「模板」页面点击「+」创建模板（输入名称和描述）
2. 点击模板卡片进入「模板详情页」
3. 点击右下角「+」添加任务到模板
4. 可以编辑或删除模板内的任务

### 应用模板到首页
1. 在「模板」页面点击「应用」按钮
2. 弹出确认对话框，选择是否「清空现有任务」
3. 确认后，模板任务复制到「今日日程」

---

## ✅ 测试验证项

修复后需要验证：

1. **任务提醒**
   - [ ] 安装APK后，首页已有任务能正常提醒
   - [ ] 新增任务能正常提醒
   - [ ] 重启手机后，任务仍能正常提醒

2. **模板功能**
   - [ ] 能创建带任务的模板
   - [ ] 点击模板卡片进入详情页
   - [ ] 在详情页能添加/编辑/删除模板任务
   - [ ] 应用模板能将任务复制到首页
   - [ ] 应用模板时可以选择是否清空现有任务
   - [ ] 导入导出功能正常

---

## 📝 数据模型说明

### Task 表
```kotlin
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val hour: Int,
    val minute: Int,
    val type: TaskType,
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val templateId: Long = 0  // 0 = 今日任务, >0 = 模板任务
)
```

### Template 表
```kotlin
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val tasksJson: String? = null  // 保留但不使用，任务通过 Task.templateId 关联
)
```

---

*报告生成时间：2026-04-28*
*修复完成时间：2026-04-28*
