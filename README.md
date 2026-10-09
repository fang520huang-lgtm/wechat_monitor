<div align="center">
  <img src="app/src/main/res/drawable-nodpi/ic_notification_card_bell_large.png" width="96" alt="消息通知闹钟图标">
  <h1>消息通知闹钟</h1>
  <p>微信、企业微信或 QQ 通知符合规则时，让手机持续响铃和/或震动，直到主动停止。</p>
</div>

## 项目简介

本 App 完全在 Android 手机本地运行，通过系统通知监听服务处理以下应用的通知：

- 微信：`com.tencent.mm`
- 企业微信：`com.tencent.wework`
- QQ：`com.tencent.mobileqq`

App 不登录上述账号，也没有联网权限。通知文字只用于当次规则判断和闹钟通知，
不会保存历史记录或上传。

## 下载

可在 [GitHub Releases](https://github.com/fang520huang-lgtm/wechat_monitor/releases)
下载已发布版本。当前源码版本为 **3.1.0**。

## 匹配规则

每条规则均可独立设置：

1. 消息来源：微信、企业微信或 QQ。
2. 匹配通知标题、通知正文，或同时匹配两者。
3. 标题和正文分别选择“完全匹配”或“部分匹配（包含）”。

同一条规则中，所有已勾选条件必须同时满足；多条规则之间满足任意一条即可。
规则卡片会把应用来源、标题条件和正文条件分行显示。

新增规则默认采用“微信 + 标题 + 完全匹配”。从 3.0.0 升级时，原有微信监听对象
会自动迁移成“微信 + 标题完全匹配”规则，不需要重新输入。

示例：

- 微信，标题完全匹配“张三”。
- 企业微信，正文部分匹配“紧急”。
- QQ，标题部分匹配“项目群”，同时正文部分匹配“上线”。

## 铃声、震动和扬声器

提醒方式支持三种组合：

- 仅铃声
- 仅震动
- 铃声 + 震动

铃声和震动至少需要开启一项。铃声可以使用系统铃声、手机中的音频文件或 App
内置铃声。

Android 9（API 28）及以上会通过系统音频路由接口优先把闹钟播放器绑定到手机
内置扬声器，避免把闹钟送入耳机；代码还会记录系统最终采用的设备，方便排查。
Android 8.x 没有等价的公开逐播放器路由接口，因此只能使用系统默认音频路由。
个别厂商系统仍可能覆盖应用请求，最终行为以实机为准。

## 使用方法

1. 安装并打开 App。
2. 在“权限与后台”中授予“通知使用权”。
3. 在“匹配规则”中选择消息来源并添加至少一条规则。
4. 在“铃声与震动”中选择提醒组合和铃声。
5. 点击“测试闹钟”，确认声音、震动和停止按钮正常。
6. 允许 App 自启动，并把后台省电策略设为“不限制”。

建议把被监听的聊天应用退到后台或锁屏后再测试。应用停留在当前聊天页面时，
部分版本可能不会产生系统通知，这种情况下本 App 也无法收到该条消息。

## 主要功能

- 首页可随时暂停或恢复消息监听。
- 支持多个跨应用规则，规则修改立即生效。
- 闹钟与震动持续循环，直到点击闹钟通知、通知中的“停止闹钟”或 App 内按钮。
- 每分钟检查通知监听的真实连接状态，异常时自动尝试重连。
- 系统启动或 App 更新后自动恢复已启用的监听服务。
- 监听关闭时停止读取目标应用通知并移除常驻状态通知。
- 铃声文件导入 App 私有目录，配置和规则只保存在本机。

## 系统要求

- Android 8.0 或更高版本，或支持安装 APK 的 HarmonyOS 设备。
- 目标聊天应用必须允许显示系统通知及通知文字。
- 使用扬声器定向路由需要 Android 9 或更高版本。

## 构建

环境：

- JDK 17 或更高版本
- Android SDK 36

Windows：

```powershell
.\gradlew.bat assembleDebug
```

macOS 或 Linux：

```bash
./gradlew assembleDebug
```

APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

通过数据线安装：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

实机烟雾测试：

```powershell
.\gradlew.bat assembleDebugAndroidTest
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.local.wechatalarm.test/com.local.wechatalarm.SmokeInstrumentation
```

## 版本

- 3.1.0：加入企业微信和 QQ；标题/正文完全或部分匹配；提醒方式组合；内置扬声器路由。
- 3.0.0：加入首页监听开关和监听服务自动恢复。

## 注意事项

- 本项目依赖聊天应用生成的系统通知，无法读取未生成通知的消息。
- 不同版本的聊天应用可能改变通知标题和正文格式，请按手机实际通知文字配置规则。
- 双开应用能否监听，取决于手机系统是否把双开通知提供给通知监听服务。
- 本项目与腾讯公司无关，仅用于个人提醒和技术学习。
- 内置测试铃声不包含在 MIT 授权范围内；公开分发前请替换为已获授权的音频。

## 开源许可

除上述内置测试铃声外，项目代码采用 [MIT 许可证](LICENSE)。
