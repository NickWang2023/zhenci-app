# 针刺 - APK 构建指南

## 方法一：使用 Android Studio（推荐）

### 1. 安装 Android Studio
```bash
# 使用 Homebrew 安装
brew install --cask android-studio
```

### 2. 打开项目
```bash
open /Users/king/Desktop/针刺-app -a "Android Studio"
```

### 3. 构建 APK
1. 等待 Gradle 同步完成
2. 点击菜单栏 **Build → Build Bundle(s) / APK(s) → Build APK(s)**
3. 构建完成后，APK 路径：
   - Debug: `app/build/outputs/apk/debug/app-debug.apk`
   - Release: `app/build/outputs/apk/release/app-release.apk`

### 4. 安装到手机
```bash
# 连接手机，开启 USB 调试
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 方法二：命令行构建

### 1. 设置环境变量
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### 2. 授予 Gradle 执行权限
```bash
cd /Users/king/Desktop/针刺-app
chmod +x gradlew
```

### 3. 构建 Debug APK
```bash
./gradlew assembleDebug
```

### 4. 构建 Release APK（需要签名）
```bash
# 生成签名密钥
keytool -genkey -v -keystore zhenci.keystore -alias zhenci -keyalg RSA -keysize 2048 -validity 10000

# 构建 Release
./gradlew assembleRelease
```

---

## 📱 APK 输出位置

| 类型 | 路径 |
|------|------|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `app/build/outputs/apk/release/app-release-unsigned.apk` |

---

## ⚠️ 注意事项

1. **首次构建**需要下载依赖，可能需要 10-30 分钟
2. **JDK 版本**要求：JDK 17 或更高
3. **Android SDK**需要安装 API 34
4. **手机权限**：安装 APK 需要开启"允许安装未知来源应用"

---

## 🔧 快速测试

构建完成后，可以用以下命令安装到手机：
```bash
adb devices                    # 查看连接的设备
adb install app-debug.apk      # 安装 APK
adb logcat                     # 查看日志
```
