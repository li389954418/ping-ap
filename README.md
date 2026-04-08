# 🌐 NetTool - 安卓网络工具

一款简洁高效的安卓网络诊断工具，使用 **Kotlin + Jetpack Compose** 构建。

## ✨ 功能特性

- **🚀 快速 Ping 测试**：输入 IP 地址或域名，一键测试网络连通性
- **💾 地址收藏**：保存常用地址到本地数据库，支持添加备注
- **🔍 智能搜索**：快速筛选已保存的地址
- **🗑️ 便捷管理**：滑动删除不需要的地址
- **📱 现代 UI**：Material Design 3 风格，简洁美观

## 🛠️ 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **本地数据库**：Room
- **异步处理**：Kotlin Coroutines + Flow
- **架构模式**：MVVM
- **依赖注入**：手动依赖注入（轻量级）

## 📋 系统要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34 (Compile SDK)
- 最低支持 Android 7.0 (API 24)

## 🚀 快速开始

### 1. 克隆/打开项目

```bash
# 在 Android Studio 中打开项目目录
```

### 2. 同步 Gradle

打开项目后，Android Studio 会自动同步 Gradle 依赖。如需手动同步：
```
File → Sync Project with Gradle Files
```

### 3. 运行应用

- **模拟器**：选择已配置的 Android 模拟器，点击 Run 按钮
- **真机调试**：开启 USB 调试，连接设备后点击 Run

### 4. 打包 APK

#### 方法一：使用脚本（推荐）

```bash
chmod +x build-apk.sh
./build-apk.sh
```

输出位置：`app/build/outputs/apk/release/app-release.apk`

#### 方法二：Android Studio

```
Build → Generate Signed Bundle / APK → APK → Release
```

#### 方法三：命令行

```bash
./gradlew assembleRelease
```

## 📁 项目结构

```
app/
├── src/main/
│   ├── java/com/example/nettool/
│   │   ├── data/              # 数据层
│   │   │   ├── Address.kt     # 实体类
│   │   │   ├── AddressDao.kt  # DAO 接口
│   │   │   └── AppDatabase.kt # 数据库配置
│   │   ├── net/               # 网络层
│   │   │   └── Pinger.kt      # Ping 工具
│   │   ├── ui/                # UI 层
│   │   │   ├── theme/         # 主题配置
│   │   │   ├── MainScreen.kt  # 主界面
│   │   │   └── MainViewModel.kt # ViewModel
│   │   └── MainActivity.kt    # 入口 Activity
│   └── AndroidManifest.xml    # 应用清单
└── build.gradle.kts           # 模块配置
```

## 🔐 签名配置（发布正式版本）

如需签名发布版本，在 `app/build.gradle.kts` 中配置：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("your-keystore.jks")
            storePassword = "your-store-password"
            keyAlias = "your-key-alias"
            keyPassword = "your-key-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## 📝 权限说明

应用需要以下权限：

- `INTERNET`：网络访问（Ping 测试必需）
- `ACCESS_NETWORK_STATE`：网络状态检查

## 🎨 界面预览

### 主界面
- 顶部：搜索栏（实时过滤地址列表）
- 中部：快速 Ping 区域（输入框 + 测试按钮 + 结果展示）
- 底部：已保存地址列表（支持滑动删除）
- 浮动按钮：将当前测试地址保存到收藏

## 🐛 常见问题

**Q: Ping 结果显示超时？**
A: 检查网络连接，或目标主机是否禁用了 ICMP 响应。

**Q: 无法保存地址？**
A: 确保 IP 地址格式正确，备注可为空。

**Q: 构建失败？**
A: 确保 JDK 版本为 17，Gradle 版本与 AGP 兼容。

## 📄 许可证

MIT License

---

**开发时间**：2026  
**技术栈版本**：Kotlin 1.9.20 | Compose BOM 2023.10.01 | Room 2.6.1