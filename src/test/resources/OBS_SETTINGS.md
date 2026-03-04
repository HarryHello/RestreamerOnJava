# OBS 媒体监测配置说明

## 新增配置字段

在 `Settings` 类中添加了以下三个字段用于配置 OBS 媒体源自动重启功能：

### 1. `doAutoRestartObsSource` (Boolean, 默认值：`true`)

是否启用 OBS 媒体源自动重启功能。

- `true`: 启用自动重启（默认）
- `false`: 禁用自动重启

**使用场景：**
- 如果需要完全手动控制媒体源，设置为 `false`
- 如果希望网络异常时自动恢复，设置为 `true`

### 2. `ObsScene` (String, 默认值：`"restreamer"`)

OBS 场景名称，媒体源所在的场景。

**说明：**
- 该配置目前主要用于日志记录
- 未来可能用于场景检测（当场景切换时自动启用/禁用监测）

### 3. `ObsSource` (String, 默认值：`"stream"`)

OBS 媒体源名称，需要监测的源。

**说明：**
- 必须与 OBS 中显示的源名称完全一致
- 对于 obs-vlc-video-plugin 插件，通常是 "Stream Link Source" 或自定义名称

## 配置方式

### 方式 1：通过 API 更新

```bash
# 更新单个设置
curl -X POST http://localhost:8080/api/settings/doAutoRestartObsSource \
  -H "Content-Type: application/json" \
  -d 'false'

# 更新多个设置
curl -X PUT http://localhost:8080/api/settings \
  -H "Content-Type: application/json" \
  -d '{
    "doAutoRestartObsSource": true,
    "ObsScene": "restreamer",
    "ObsSource": "stream"
  }'
```

### 方式 2：直接修改 settings.json

```json
{
  "doAutoRestartObsSource": true,
  "ObsScene": "restreamer",
  "ObsSource": "stream",
  ...其他设置
}
```

## 应用启动时的行为

应用启动时，`ObsMediaMonitorInitializer` 会：

1. 读取 `Settings` 中的配置
2. 如果 `doAutoRestartObsSource = true` 且 `ObsSource` 不为空
3. 自动启动媒体监测，每 10 秒检查一次
4. 如果检测到源停止/暂停，自动触发重启

### 启动日志示例

```
[INFO] Media monitor initialized for source: 'stream' (interval: 10s)
```

如果禁用自动重启：

```
[INFO] Auto-restart is disabled in settings, skipping media monitor initialization
```

## 运行时控制

即使启动了自动监测，仍然可以在运行时控制：

### 禁用自动重启

```java
@Autowired
private ObsMediaMonitor mediaMonitor;

// 临时禁用
mediaMonitor.setAutoRestartEnabled("stream", false);
```

### 启用自动重启

```java
// 重新启用
mediaMonitor.setAutoRestartEnabled("stream", true);
```

### 完全停止监测

```java
mediaMonitor.stopMonitoring("stream");
```

## 完整配置示例

```json
{
  "ChannelId": "UCxsZ6NCzjU_t4YSxQLBcM5A",
  "translationProducer": "ollama",
  "doAutoRestartObsSource": true,
  "ObsScene": "restreamer",
  "ObsSource": "stream",
  "ollamaConfig": {
    "host": "localhost",
    "port": 11434,
    "model": "llama2"
  },
  ...
}
```

## 注意事项

1. **源名称必须准确** - `ObsSource` 必须与 OBS 中显示的源名称完全一致（区分大小写）
2. **场景名称可选** - `ObsScene` 目前主要用于日志，不影响监测逻辑
3. **默认启用** - 如果不配置，`doAutoRestartObsSource` 默认为 `true`
4. **监测间隔固定** - 目前固定为 10 秒，未来可能改为可配置
