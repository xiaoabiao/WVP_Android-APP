# WVP Android-APP — 国标设备管理客户端

基于 [WVP-PRO](https://github.com/648540858/wvp-GB28181-pro) 平台的 Android 管理客户端，用于对 GB/T 28181 国标设备进行远程管理、通道查看和视频点播。

## 功能一览

| 功能 | 说明 |
|------|------|
| 🔐 **登录认证** | 连接 WVP-PRO 服务端，支持记住密码 |
| 📡 **设备列表** | 分页获取设备列表，支持搜索、刷新、在线状态标识 |
| 📋 **通道管理** | 查看设备下所有通道，搜索过滤通道 |
| ▶️ **视频点播** | 通过 ExoPlayer 播放 RTSP/RTMP/HLS 流，支持全屏 |
| ⚙️ **设备操作** | 修改设备名称、修改收流 IP、同步目录、切换 TCP/UDP 传输模式 |

## 技术栈

| 层 | 技术 |
|----|------|
| 语言 | **Kotlin** |
| UI | **Material Design 3** (MaterialComponents), **ViewBinding** |
| 架构 | **MVVM** (Lifecycle + LiveData + ViewModel), **Coroutines** |
| 网络 | **Retrofit 2** + **OkHttp 4** + **Gson** |
| 播放器 | **Media3 ExoPlayer** (支持 RTSP) |
| 构建 | **Gradle 8.7** + **AGP 8.4.0** |

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Gradle 8.7 (自动下载)
- Android SDK 34

## 快速开始

```bash
# 克隆仓库
git clone https://github.com/xiaoabiao/WVP_Android-APP.git
cd WVP_Android-APP

# 编译调试包
./gradlew assembleDebug

# 编译正式包
./gradlew assembleRelease
```

生成的 APK 位于 `app/build/outputs/apk/` 目录下。

### 用 Android Studio 打开

1. `File` → `Open...` → 选择 `WVP_Android-APP` 目录
2. 等待 Gradle 同步完成
3. `Run` → `Run 'app'` 或直接点击 ▶ 按钮

## 配置说明

应用默认连接 WVP-PRO 服务端，首次启动时需填写：

- **服务地址**：WVP-PRO 的 HTTP 访问地址（如 `http://192.168.1.100:18080`）
- **用户名 / 密码**：WVP-PRO 平台的登录凭证

## 构建产物

项目已内置的 APK 在 `app/release/` 目录下：
- `WVP管理工具v1.3.apk` — 可直接安装的 Release 包

## 预览截图

<img width="1920" height="1080" alt="未标题-2" src="https://github.com/user-attachments/assets/c982c888-7e1c-4959-9c4f-4594c0530ff9" />




## 许可证

本项目基于 [GNU General Public License v2](LICENSE) 发布。
