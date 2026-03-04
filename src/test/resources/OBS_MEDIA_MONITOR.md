# OBS 媒体监测器使用指南

## 功能概述

`ObsMediaMonitor` 用于监测 OBS 中的媒体源（如 Stream Link Source）状态，并在异常停止时自动重启。

### 主要特性

1. **定期监测** - 按指定间隔检查媒体源状态
2. **自动恢复** - 检测到异常停止/暂停时自动重启媒体源
3. **启用/禁用控制** - 可随时启用或禁用自动重启功能

### 工作原理

```
监测循环 (每 N 秒)
    ↓
检查自动重启是否启用？
    ├─ 否 → 仅更新状态，不重启
    └─ 是 → 继续检查
         ↓
    获取当前媒体状态
    (PLAYING/PAUSED/STOPPED)
         ↓
    之前是 PLAYING 且现在是 STOPPED/PAUSED？
         ├─ 是 → 触发 RESTART 动作
         └─ 否 → 不操作
```

## 使用方式

### 1. 注入 ObsMediaMonitor

```java
@Autowired
private ObsMediaMonitor mediaMonitor;
```

### 2. 启动监测

```java
// 启动监测，每 10 秒检查一次
mediaMonitor.startMonitoring("stream", 10);
```

### 3. 禁用/启用自动重启

当需要临时禁用自动重启时（例如用户要在 OBS 中手动操作）：

```java
// 禁用自动重启
mediaMonitor.setAutoRestartEnabled("stream", false);

// ... 用户进行操作 ...

// 启用自动重启
mediaMonitor.setAutoRestartEnabled("stream", true);
```

### 4. 停止监测

```java
mediaMonitor.stopMonitoring("stream");
```

## 完整示例

### 在 Controller 中使用

```java
@RestController
@RequestMapping("/api/media")
public class MediaController {
    
    @Autowired
    private ObsMediaMonitor mediaMonitor;
    
    @Autowired
    private ObsService obsService;
    
    /**
     * 开始监测并设置直播链接
     */
    @PostMapping("/start")
    public ResponseEntity<String> start(@RequestBody Map<String, String> params) {
        String sourceName = params.get("sourceName");
        String streamUrl = params.get("streamUrl");
        
        // 启动监测（每 10 秒检查一次）
        mediaMonitor.startMonitoring(sourceName, 10);
        
        // 设置直播链接
        obsService.setStreamLinkSourceUrl(sourceName, streamUrl)
            .thenAccept(response -> {
                log.info("直播链接设置成功");
            })
            .exceptionally(ex -> {
                log.error("设置失败", ex);
                return null;
            });
        
        return ResponseEntity.ok("Started");
    }
    
    /**
     * 临时禁用自动重启（用户要在 OBS 中手动操作）
     */
    @PostMapping("/disable-auto-restart")
    public ResponseEntity<String> disableAutoRestart(@RequestBody Map<String, String> params) {
        String sourceName = params.get("sourceName");
        
        // 禁用自动重启
        mediaMonitor.setAutoRestartEnabled(sourceName, false);
        
        return ResponseEntity.ok("Auto-restart disabled");
    }
    
    /**
     * 启用自动重启
     */
    @PostMapping("/enable-auto-restart")
    public ResponseEntity<String> enableAutoRestart(@RequestBody Map<String, String> params) {
        String sourceName = params.get("sourceName");
        
        // 启用自动重启
        mediaMonitor.setAutoRestartEnabled(sourceName, true);
        
        return ResponseEntity.ok("Auto-restart enabled");
    }
    
    /**
     * 停止监测
     */
    @PostMapping("/stop")
    public ResponseEntity<String> stop(@RequestBody Map<String, String> params) {
        String sourceName = params.get("sourceName");
        
        mediaMonitor.stopMonitoring(sourceName);
        
        return ResponseEntity.ok("Stopped");
    }
}
```

## 关于用户手动操作的说明

**重要**：该监测器**无法区分**用户手动操作（在 OBS 界面点击暂停/停止）和异常停止（网络问题、插件崩溃等）。

### 如何处理用户手动操作？

**方案 1：临时禁用自动重启**

在用户需要在 OBS 中手动操作前，调用 API 禁用自动重启：

```java
// 用户准备操作
mediaMonitor.setAutoRestartEnabled("stream", false);

// 用户在 OBS 中点击暂停/停止...

// 操作完成后重新启用
mediaMonitor.setAutoRestartEnabled("stream", true);
```

**方案 2：停止监测**

如果用户需要长时间手动控制，可以完全停止监测：

```java
mediaMonitor.stopMonitoring("stream");
```

**方案 3：场景联动（推荐）**

当切换到不包含该源的场景时，自动禁用监测：

```java
// 在场景切换逻辑中
if (!currentScene.containsSource("stream")) {
    mediaMonitor.setAutoRestartEnabled("stream", false);
} else {
    mediaMonitor.setAutoRestartEnabled("stream", true);
}
```

## 状态枚举

```java
public enum SourceMediaState {
    PLAYING,    // 播放中
    PAUSED,     // 暂停
    STOPPED,    // 停止
    UNKNOWN     // 未知
}
```

## API 参考

### 启动监测

```java
void startMonitoring(String sourceName, long checkIntervalSeconds)
```

- `sourceName`: OBS 中源的名称
- `checkIntervalSeconds`: 检查间隔（秒），建议 5-30 秒

### 停止监测

```java
void stopMonitoring(String sourceName)
```

### 启用/禁用自动重启

```java
void setAutoRestartEnabled(String sourceName, boolean enabled)
boolean isAutoRestartEnabled(String sourceName)
```

### 检查监测状态

```java
boolean isMonitoring(String sourceName)
```

## 注意事项

1. **OBS 连接状态** - 确保 ObsService 已连接且 `isReady()` 返回 true
2. **源名称匹配** - 确保传入的源名称与 OBS 中显示的完全一致
3. **检查间隔** - 建议设置为 5-30 秒
   - 太短：增加系统和网络负担
   - 太长：恢复延迟
4. **用户操作** - 在用户需要手动操作前，记得调用 `setAutoRestartEnabled(sourceName, false)` 禁用自动重启
5. **多个源** - 可以为不同的源设置不同的监测间隔

## 日志示例

```
[INFO] Starting media monitor for source: stream (interval: 10s)
[INFO] Detected abnormal stop for source 'stream'
[INFO] Attempting to recover source 'stream' (state: STOPPED, previous: PLAYING)
[INFO] Sending media action 'OBS_WEBSOCKET_MEDIA_INPUT_ACTION_RESTART' for input 'stream'
[INFO] Media action 'OBS_WEBSOCKET_MEDIA_INPUT_ACTION_RESTART' successfully triggered for input 'stream'
[INFO] Source 'stream' restarted successfully (visibility + media control)
[INFO] Auto-restart disabled for source: stream
[INFO] Auto-restart enabled for source: stream
```
