#!/bin/bash

# 针刺 APK 自动构建脚本
# 使用方法: ./build-apk.sh [debug|release]

set -e

echo "🦞 针刺 - APK 自动构建脚本"
echo "=========================="

# 检查 Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  未设置 ANDROID_HOME 环境变量"
    echo "尝试查找 Android SDK..."
    
    # 常见安装路径
    if [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    elif [ -d "/usr/local/share/android-sdk" ]; then
        export ANDROID_HOME="/usr/local/share/android-sdk"
    else
        echo "❌ 未找到 Android SDK"
        echo ""
        echo "请安装 Android Studio:"
        echo "  brew install --cask android-studio"
        echo ""
        echo "或手动设置 ANDROID_HOME:"
        echo "  export ANDROID_HOME=/path/to/android-sdk"
        exit 1
    fi
    
    echo "✅ 找到 Android SDK: $ANDROID_HOME"
    export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
fi

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "❌ 未找到 Java"
    echo "请安装 JDK 17:"
    echo "  brew install openjdk@17"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "✅ Java 版本: $JAVA_VERSION"

# 进入项目目录
cd "$(dirname "$0")"

# 确保 Gradle wrapper 可执行
chmod +x gradlew

# 构建类型
BUILD_TYPE=${1:-debug}

echo ""
echo "🔨 开始构建 $BUILD_TYPE APK..."
echo "=========================="

if [ "$BUILD_TYPE" = "release" ]; then
    # Release 构建
    ./gradlew assembleRelease
    
    echo ""
    echo "✅ Release APK 构建完成!"
    echo ""
    echo "📱 APK 位置:"
    echo "  app/build/outputs/apk/release/app-release-unsigned.apk"
    echo ""
    echo "⚠️  注意: Release APK 需要签名才能安装"
    echo "  签名命令:"
    echo "    jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore my.keystore app-release-unsigned.apk alias_name"
    
else
    # Debug 构建
    ./gradlew assembleDebug
    
    echo ""
    echo "✅ Debug APK 构建完成!"
    echo ""
    echo "📱 APK 位置:"
    echo "  $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    
    # 检查是否有连接的设备
    if command -v adb &> /dev/null; then
        DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
        if [ "$DEVICES" -gt 0 ]; then
            echo "📲 检测到 $DEVICES 台设备连接"
            read -p "是否直接安装到设备? (y/n): " INSTALL
            if [ "$INSTALL" = "y" ]; then
                echo "正在安装..."
                adb install -r app/build/outputs/apk/debug/app-debug.apk
                echo "✅ 安装完成!"
            fi
        fi
    fi
fi

echo ""
echo "🎉 构建流程结束!"
echo ""
