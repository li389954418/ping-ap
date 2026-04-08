#!/bin/bash

# 安卓网络工具 App - 一键打包脚本
# 使用方法：./build-apk.sh

set -e

echo "🔧 开始构建 NetTool APK..."

# 检查 Gradle 是否存在
if [ ! -f "./gradlew" ]; then
    echo "❌ 错误：gradlew 不存在，请确保在 Android 项目根目录运行"
    exit 1
fi

# 赋予执行权限
chmod +x ./gradlew

# 清理并构建 Release 版本
echo "📦 编译 Release 版本..."
./gradlew clean assembleRelease

# 检查输出文件
APK_PATH="app/build/outputs/apk/release/app-release.apk"
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "✅ 构建成功！"
    echo "📍 APK 位置：$APK_PATH"
    echo ""
    echo "💡 安装到设备："
    echo "   adb install $APK_PATH"
    echo ""
    
    # 显示文件大小
    ls -lh "$APK_PATH" | awk '{print "📊 文件大小："$5}'
else
    echo "❌ 构建失败：未找到输出文件"
    exit 1
fi