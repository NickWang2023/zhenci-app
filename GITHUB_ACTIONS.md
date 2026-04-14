# GitHub Actions 自动构建

本项目已配置 GitHub Actions 工作流，可自动构建 APK 并发布到 GitHub Releases。

## 🚀 使用方法

### 1. 创建 GitHub 仓库

```bash
# 在 GitHub 上创建新仓库，例如：zhenci-app

# 初始化本地仓库
cd /Users/king/Desktop/针刺-app
git init
git add .
git commit -m "Initial commit"

# 关联远程仓库
git remote add origin https://github.com/你的用户名/zhenci-app.git
git branch -M main
git push -u origin main
```

### 2. 触发自动构建

推送代码到 main 分支后，GitHub Actions 会自动开始构建：

```bash
git push origin main
```

### 3. 查看构建状态

1. 打开 GitHub 仓库页面
2. 点击 **Actions** 标签
3. 查看构建进度和日志

### 4. 下载 APK

构建完成后，APK 会以两种方式提供：

#### 方式一：Artifacts（每次构建）
- 进入 Actions 页面
- 点击最新的工作流运行
- 在 **Artifacts** 部分下载 APK

#### 方式二：GitHub Releases（自动发布）
- 每次推送到 main 分支会自动创建 Release
- 进入仓库的 **Releases** 页面
- 下载最新版本的 APK

---

## 📋 工作流说明

### 触发条件

工作流在以下情况自动运行：
- ✅ 推送到 `main` 或 `master` 分支
- ✅ 提交 Pull Request
- ✅ 手动触发（在 Actions 页面点击 "Run workflow"）

### 构建产物

| 类型 | 文件名 | 说明 |
|------|--------|------|
| Debug | `app-debug.apk` | 测试版本，可直接安装 |
| Release | `app-release-unsigned.apk` | 发布版本，需签名后安装 |

---

## 🔧 高级配置

### 签名 Release APK

如需自动签名 Release APK，需要配置密钥：

1. 生成本地密钥库
```bash
keytool -genkey -v -keystore zhenci.keystore -alias zhenci -keyalg RSA -keysize 2048 -validity 10000
```

2. 将密钥转为 Base64
```bash
base64 -i zhenci.keystore | pbcopy
```

3. 在 GitHub 仓库设置中添加 Secrets
   - 打开 Settings → Secrets and variables → Actions
   - 添加 `KEYSTORE_BASE64`: 粘贴 Base64 编码的密钥
   - 添加 `KEYSTORE_PASSWORD`: 密钥库密码
   - 添加 `KEY_ALIAS`: 密钥别名
   - 添加 `KEY_PASSWORD`: 密钥密码

4. 更新工作流文件启用签名（已注释在 build-apk.yml 中）

---

## 🐛 故障排除

### 构建失败

1. 检查 Gradle 配置
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. 查看详细日志
   - 在 GitHub Actions 页面点击失败的构建
   - 查看具体错误信息

### 缓存问题

如果依赖更新后构建失败，尝试清除缓存：
- 在 Actions 页面点击 "Clear caches"
- 或修改工作流文件中的 cache key

---

## 📱 安装 APK

下载 APK 后，可通过以下方式安装：

### 方式一：ADB
```bash
adb install app-debug.apk
```

### 方式二：直接安装
1. 将 APK 发送到手机
2. 点击安装（允许"未知来源"）

---

**享受云端自动构建的便利！** 🦞
