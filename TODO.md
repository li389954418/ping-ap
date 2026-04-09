# 安卓网络工具 App

**目标：** 一款可快速 Ping 并收藏地址的轻量安卓工具  
**技术栈：** Kotlin + Jetpack Compose + Room + Coroutines

---

## Task 1: 项目骨架与依赖配置

> 初始化 Android 项目，引入 Compose、Room、Coroutines 等关键依赖，配置 APK 打包

**文件：**  
- `app/build.gradle.kts`  
- `settings.gradle.kts`  
- `gradle/libs.versions.toml`

- [x] 创建 Empty Compose Activity 模板  
- [x] 配置 Compose BOM、Room、Coroutines、Junit 等依赖  
- [x] 开启 ViewBinding 关闭、Compose 编译器版本对齐  
- [x] 配置 `assembleRelease` 一键打包任务，签名信息留占位

---

## Task 2: 数据层 — 地址实体与 DAO

> 定义本地数据库表结构，提供增删查接口

**文件：**  
- `app/src/main/java/com/example/nettool/data/Address.kt`  
- `app/src/main/java/com/example/nettool/data/AddressDao.kt`  
- `app/src/main/java/com/example/nettool/data/AppDatabase.kt`

- [x] `Address` 实体：id、ip、备注、创建时间  
- [x] `AddressDao`：insert、delete、getAll、searchByIp/Note  
- [x] `AppDatabase` 单例 + Room 构建，导出 schema 关闭

---

## Task 3: 网络层 — Ping 工具封装

> 在子进程执行 ping 命令，返回结果与耗时

**文件：**  
- `app/src/main/java/com/example/nettool/net/Pinger.kt`

- [x] `suspend fun ping(host: String, count: Int = 4): PingResult`  
- [x] 使用 `ProcessBuilder` 执行 `/system/bin/ping -c 4 <host>`  
- [x] 解析 stdout：丢包率、最小/平均/最大延迟  
- [x] 异常处理：UnknownHost、超时、无网络权限等

---

## Task 4: UI 层 — 主界面 Compose 布局

> 单屏实现：顶部快速 Ping 区 + 下方地址列表，含搜索与删除

**文件：**  
- `app/src/main/java/com/example/nettool/ui/MainScreen.kt`  
- `app/src/main/java/com/example/nettool/ui/theme/Theme.kt`

- [x] Scaffold + TopBar：标题与搜索框（实时过滤列表）  
- [x] 快速 Ping 卡片：TextField(IP/域名) + Button(测试) + 结果文本  
- [x] 地址列表：LazyColumn + Card，显示 IP、备注、延迟标签  
- [x] 侧滑删除：SwipeToDismiss 调用 DAO delete  
- [x] 浮动按钮：点击将当前 Ping 的 IP 带备注保存到数据库  
- [x] 整体 Material3 浅色主题，简洁圆角卡片风格

---

## Task 5: ViewModel & State 管理

> 连接 UI 与数据/网络层，保持配置变更安全

**文件：**  
- `app/src/main/java/com/example/nettool/ui/MainViewModel.kt`

- [x] 持有 AddressDao 与 Pinger 依赖，注入 Database 实例  
- [x] `uiState: StateFlow<MainUiState>` 包含：  
  – 搜索关键字、地址列表、Ping 结果、加载/错误状态  
- [x] `ping(host: String)` 协程调用 Pinger，更新状态  
- [x] `saveCurrent(ip: String, note: String)` 插入数据库  
- [x] `delete(address: Address)` 删除并刷新列表  
- [x] `search(keyword: String)` 实时过滤本地列表

---

## Task 6: 权限与清单配置

> 确保能访问网络与执行 shell ping

**文件：**  
- `app/src/main/AndroidManifest.xml`

- [x] 添加 `<uses-permission android:name="android.permission.INTERNET"/>`  
- [x] 添加 `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>`  
- [x] targetSdk 34，compileSdk 34，minSdk 24（Compose 最低）

---

## Task 7: 一键打包脚本与说明

> 本地或 CI 均可一键输出 release APK

**文件：**  
- `build-apk.sh`  
- `README.md`

- [x] 脚本内容：`./gradlew assembleRelease`  
- [x] 输出路径：`app/build/outputs/apk/release/app-release.apk`  
- [x] README 写明：环境要求、签名配置方法、安装测试步骤