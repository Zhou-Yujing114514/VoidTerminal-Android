# 虚空终端 - 聊天APP

纯原生 Android 聊天应用，对接 buer.kdns.fr 后端。

## 功能

- 登录/注册
- 公共大厅聊天
- 私聊
- 群聊（创建群聊、群聊消息）
- 好友系统（通讯录、在线状态）
- 朋友圈（发布动态、图片、点赞、评论、删除）
- 图片消息（发送、查看）
- 头像上传
- 消息撤回
- 站长管理（发布公告、封禁/解封、踢出用户、重命名大厅、清空大厅、设置在线人数上限）
- 公告页（聊天列表顶部横幅 + 公告列表）
- 消息通知

## 技术栈

- Java
- AndroidX
- OkHttp WebSocket
- Glide（图片加载）
- Material Components

## 在 AndroidIDE 中编译

### 环境要求
- AndroidIDE 最新版
- JDK 17
- Android SDK (compileSdk 34, minSdk 24)
- Gradle 8.2

### 步骤

1. **打开项目**
   - 将整个 ChatApp 文件夹复制到手机
   - AndroidIDE → 打开项目 → 选择 ChatApp 文件夹

2. **配置 SDK**
   - AndroidIDE → 设置 → SDK → 配置 Android SDK 路径
   - 确保已安装 Android 14 (API 34) SDK Platform

3. **同步项目**
   - 打开后 AndroidIDE 会自动同步 Gradle
   - 等待依赖下载完成（Glide、OkHttp、Material 等）

4. **构建 APK**
   - 底部终端执行：
     ```bash
     gradle assembleDebug
     ```
   - 或点击右上角构建按钮

5. **APK 输出位置**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### 注意事项

- 首次构建需要下载 Gradle 8.2 和依赖，需要网络
- 如果 `local.properties` 不存在，AndroidIDE 会自动生成
- 默认服务器地址：https://buer.kdns.fr，可在登录页修改
- 版本号：versionCode 4, versionName 2.2

## 项目结构

```
ChatApp/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/chatapp/
│       │   ├── LoginActivity.java
│       │   ├── MainActivity.java
│       │   ├── ChatActivity.java
│       │   ├── PostMomentActivity.java
│       │   ├── ImageViewerActivity.java
│       │   ├── CreateGroupActivity.java
│       │   ├── AdminActivity.java
│       │   ├── AnnouncementActivity.java
│       │   ├── adapter/
│       │   │   ├── ChatListAdapter.java
│       │   │   ├── MessageAdapter.java
│       │   │   ├── FriendAdapter.java
│       │   │   └── MomentAdapter.java
│       │   ├── fragment/
│       │   │   ├── ChatListFragment.java
│       │   │   ├── ContactsFragment.java
│       │   │   ├── MomentsFragment.java
│       │   │   └── ProfileFragment.java
│       │   ├── model/
│       │   │   ├── User.java
│       │   │   ├── Message.java
│       │   │   ├── ChatRoom.java
│       │   │   ├── Group.java
│       │   │   └── Moment.java
│       │   ├── api/
│       │   │   └── ApiClient.java
│       │   ├── util/
│       │   │   └── SharedPrefs.java
│       │   └── websocket/
│       │       └── WebSocketManager.java
│       └── res/
│           ├── layout/
│           ├── drawable/
│           ├── values/
│           └── menu/
├── build.gradle
├── settings.gradle
├── gradle.properties
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

## 后端 API

- WebSocket: `wss://buer.kdns.fr/ws?token=xxx`
- 登录: `POST /api/login`
- 注册: `POST /api/register`
- 上传消息图片: `POST /api/upload-msg-image`
- 上传头像: `POST /api/avatar`
- 发布朋友圈: `POST /api/moment-post`
