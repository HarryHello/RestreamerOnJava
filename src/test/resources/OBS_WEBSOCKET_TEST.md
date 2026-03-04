# OBS WebSocket 测试说明

## 配置文件

测试配置文件位于：`src/test/resources/test-obs.properties`

该文件已添加到 `.gitignore`，**不会被提交到 Git 仓库**。

## 配置项

编辑 `src/test/resources/test-obs.properties` 文件，填入你的 OBS WebSocket 连接信息：

```properties
# OBS WebSocket 服务器地址
obs.websocket.host=localhost

# OBS WebSocket 端口（默认 4455）
obs.websocket.port=4455

# OBS WebSocket 密码
obs.websocket.password=your_password_here

# 要测试的场景名称
obs.websocket.sceneName=restreamer

# 要测试的 Stream Link Source 源名称
obs.websocket.sourceName=stream
```

## 运行测试

### 前提条件

1. **OBS Studio** 已安装并运行
2. **obs-vlc-video-plugin** 插件已安装
3. **OBS WebSocket** 已启用并配置好密码
4. 在 OBS 中创建一个名为 `restreamer` 的场景
5. 在该场景中添加一个 **Stream Link Source** 源，命名为 `stream`

### 运行测试

```bash
# 使用 Maven Wrapper 运行测试
.\mvnw.cmd test -Dtest=ObsServiceTest

# 或者运行单个测试方法
.\mvnw.cmd test -Dtest=ObsServiceTest#testSetStreamLinkSourceUrl
```

## 测试说明

### `testSetStreamLinkSourceUrl`

测试设置 Stream Link Source 的直播链接功能。

- 连接到 OBS WebSocket
- 设置 `stream` 源的 URL 为测试链接
- 验证设置是否成功

### `testSetStreamLinkSourceUrl_MultipleUrls`

测试快速切换多个直播链接。

- 依次设置多个不同的 Twitch 频道链接
- 验证每个链接都能成功设置

### `testConnection`

测试 OBS WebSocket 连接并获取版本信息。

- 验证 OBS 连接状态
- 获取 OBS 和 obs-websocket 的版本号

## 故障排除

### 1. 连接失败

- 检查 OBS 是否运行
- 确认 WebSocket 端口（默认 4455）未被防火墙阻止
- 验证密码是否正确

### 2. 设置源失败

- 确认场景名称 `restreamer` 存在
- 确认源名称 `stream` 存在且类型为 **Stream Link Source**
- 检查源名称是否拼写正确（区分大小写）

### 3. 插件未安装

如果未安装 obs-vlc-video-plugin 插件：

1. 访问：https://github.com/Chimildic/obs-vlc-video-plugin
2. 下载并安装插件
3. 重启 OBS

## 在代码中使用

```java
@Autowired
private ObsService obsService;

// 在直播开始时设置直播链接
String streamUrl = "https://www.twitch.tv/your_channel";
obsService.setStreamLinkSourceUrl("stream", streamUrl)
    .thenAccept(response -> {
        log.info("直播链接设置成功");
        return obsService.startStream();
    })
    .exceptionally(ex -> {
        log.error("设置失败", ex);
        return null;
    });
```
