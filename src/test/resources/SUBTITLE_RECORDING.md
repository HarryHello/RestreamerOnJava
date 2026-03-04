# 字幕记录功能使用指南

## 功能概述

该功能在直播过程中自动记录原文和翻译字幕，保存为 SRT 格式文件。

### 特性

1. **SRT 格式** - 标准字幕格式，兼容所有主流播放器
2. **双文件记录** - 原文和翻译分别保存
3. **智能时间戳** - 基于 60 帧/秒计算，自动估算持续时间
4. **自动命名** - 包含直播开始时间、频道名称、直播链接
5. **API 导出** - 提供完整的文件管理 API

## 文件命名规则

```
[yyyy-MM-dd-HH:mm:ss]_source_{channelName}_{streamLink}.srt
[yyyy-MM-dd-HH:mm:ss]_translate_{channelName}_{streamLink}.srt
```

**示例：**
```
[2026-02-27-10:30:00]_source_monstercat_https___www_twitch_tv_monstercat.srt
[2026-02-27-10:30:00]_translate_monstercat_https___www_twitch_tv_monstercat.srt
```

**说明：**
- 时间格式：`[yyyy-MM-dd-HH:mm:ss]`（中括号包裹，时分秒用冒号分隔）
- `channelName`：频道名称（特殊字符替换为下划线）
- `streamLink`：直播链接（特殊字符替换为下划线）

## 存储位置

字幕文件保存在程序运行目录的 `subtitles` 子目录下：

```
RestreamerOnJava/
├── subtitles/
│   ├── 2026-02-27-10-30-00_source_monstercat_....srt
│   └── 2026-02-27-10-30-00_translate_monstercat_....srt
└── ...
```

## SRT 文件格式

```srt
1
00:00:01,000 --> 00:00:04,000
Hello, this is the first subtitle

2
00:00:04,500 --> 00:00:07,500
This is the second subtitle
```

### 时间戳计算逻辑

1. **结束时间** = 接收到字幕的时间 - 直播开始时间
2. **开始时间** = max(估计值，上条字幕的结束时间)
   - 估计值 = 结束时间 - 持续时间
   - 持续时间基于文本长度估算（每个字符约 2 帧 @ 60fps）
3. **持续时间** = min(8 秒，max(2 秒，字符数 × 2 帧 / 60fps))

## API 接口

### 1. 获取字幕文件列表

```http
GET /api/subtitles
```

**响应示例：**
```json
[
  {
    "filename": "2026-02-27-10-30-00_source_monstercat.srt",
    "path": "subtitles\\2026-02-27-10-30-00_source_monstercat.srt",
    "size": 12345,
    "lastModified": "2026-02-27T10:30:00Z",
    "timestamp": "2026-02-27-10-30-00",
    "type": "source",
    "channelName": "monstercat",
    "streamLink": "https___www_twitch_tv_monstercat"
  }
]
```

### 2. 获取字幕内容

```http
GET /api/subtitles/{filename}
```

**响应：** SRT 文件内容（纯文本）

### 3. 下载字幕文件

```http
GET /api/subtitles/{filename}/download
```

**响应：** 文件下载（attachment）

### 4. 删除字幕文件

```http
DELETE /api/subtitles/{filename}
```

**响应：**
- `200 OK`: "Deleted: filename.srt"
- `404 Not Found`: 文件不存在

### 5. 批量删除

```http
POST /api/subtitles/batch-delete
Content-Type: application/json

{
  "filenames": ["file1.srt", "file2.srt"]
}
```

### 6. 按类型筛选

```http
GET /api/subtitles/filter/{type}
```

**type:** `source` 或 `translate`

### 7. 按频道筛选

```http
GET /api/subtitles/channel/{channelName}
```

## 在代码中使用

### 开始录制字幕

```java
@Autowired
private SubtitleService subtitleService;

// 直播开始时
public void onStreamStart(String channelId, String channelName, String streamLink) {
    subtitleService.startRecording(channelId, channelName, streamLink);
}
```

### 停止录制字幕

```java
// 直播结束时
public void onStreamEnd(String channelId) {
    subtitleService.stopRecording(channelId);
}
```

### 写入字幕

```java
// 写入原文
public void onSourceSubtitle(String channelId, String text) {
    subtitleService.writeSource(channelId, text);
}

// 写入翻译
public void onTranslateSubtitle(String channelId, String text, long sourceReceivedTime) {
    subtitleService.writeTranslate(channelId, text, sourceReceivedTime);
}

// 同时写入原文和翻译（推荐，保持时间戳同步）
public void onBothSubtitles(String channelId, String sourceText, String translateText) {
    subtitleService.writeBoth(channelId, sourceText, translateText);
}
```

## 集成示例

### 与 YouTube 直播字幕集成

```java
@Service
public class YoutubeSubtitleReceiver {
    
    @Autowired
    private SubtitleService subtitleService;
    
    @Autowired
    private SettingsManager settingsManager;
    
    private String channelId;
    private String channelName;
    private String streamLink;
    
    /**
     * 开始接收字幕
     */
    public void startReceiving(String channelId) {
        this.channelId = channelId;
        
        // 获取频道名称和直播链接
        Settings settings = settingsManager.getSettings();
        this.channelName = getChannelName(channelId);  // 需要实现
        this.streamLink = "https://www.youtube.com/watch?v=" + getVideoId();  // 需要实现
        
        // 开始录制
        subtitleService.startRecording(channelId, channelName, streamLink);
    }
    
    /**
     * 接收到 YouTube 字幕
     */
    public void onSubtitleReceived(SubtitleEvent event) {
        if (!subtitleService.isRecording(channelId)) {
            return;
        }
        
        String sourceText = event.getText();
        
        // 写入原文
        subtitleService.writeSource(channelId, sourceText);
        
        // 如果需要翻译，调用翻译服务
        translateAndWrite(channelId, sourceText, event.getTimestamp());
    }
    
    /**
     * 翻译并写入
     */
    private void translateAndWrite(String channelId, String sourceText, long timestamp) {
        // 调用翻译服务...
        String translatedText = translate(sourceText);
        
        // 写入翻译（保持时间戳同步）
        subtitleService.writeTranslate(channelId, translatedText, timestamp);
    }
    
    /**
     * 停止接收字幕
     */
    public void stopReceiving() {
        if (channelId != null) {
            subtitleService.stopRecording(channelId);
        }
    }
}
```

### 与 OBS 媒体监测集成

```java
@Component
public class StreamMonitor {
    
    @Autowired
    private ObsMediaMonitor mediaMonitor;
    
    @Autowired
    private SubtitleService subtitleService;
    
    @Autowired
    private SettingsManager settingsManager;
    
    /**
     * 检测到直播开始
     */
    public void onStreamDetected(String channelId, String channelName, String streamLink) {
        // 启动媒体监测
        mediaMonitor.startMonitoring(settingsManager.getSettings().getObsSource(), 10);
        
        // 开始录制字幕
        subtitleService.startRecording(channelId, channelName, streamLink);
        
        log.info("Stream started, subtitle recording enabled");
    }
    
    /**
     * 检测到直播结束
     */
    public void onStreamEnded(String channelId) {
        // 停止媒体监测
        mediaMonitor.stopMonitoring(settingsManager.getSettings().getObsSource());
        
        // 停止录制字幕
        subtitleService.stopRecording(channelId);
        
        log.info("Stream ended, subtitle recording stopped");
    }
}
```

## 前端使用示例

### Vue.js 示例

```vue
<template>
  <div>
    <button @click="downloadSubtitle(file.filename)" v-for="file in files" :key="file.filename">
      {{ file.filename }} ({{ formatSize(file.size) }})
    </button>
  </div>
</template>

<script>
export default {
  data() {
    return {
      files: []
    }
  },
  mounted() {
    this.fetchSubtitles()
  },
  methods: {
    async fetchSubtitles() {
      const response = await fetch('/api/subtitles')
      this.files = await response.json()
    },
    async downloadSubtitle(filename) {
      const response = await fetch(`/api/subtitles/${filename}/download`)
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      window.URL.revokeObjectURL(url)
    },
    formatSize(bytes) {
      return (bytes / 1024).toFixed(2) + ' KB'
    }
  }
}
</script>
```

## 注意事项

1. **文件名安全** - 频道名称和直播链接中的特殊字符会被替换为下划线
2. **时间戳精度** - 基于 60 帧/秒计算，适用于大多数视频播放器
3. **文件管理** - 定期清理旧字幕文件，避免占用过多磁盘空间
4. **并发录制** - 支持多个频道同时录制字幕
5. **异常处理** - 如果写入失败，不会中断直播流程

## 故障排除

### 字幕文件未创建

检查：
1. `subtitles` 目录是否有写入权限
2. 频道名称是否包含非法字符
3. 日志中是否有错误信息

### 时间戳不正确

确保：
1. 直播开始时间正确记录
2. 字幕接收时间与直播开始时间的相对关系正确
3. 文本长度估算合理（可调整 `FRAMES_PER_CHAR` 常量）

### 文件无法下载

检查：
1. 文件名是否正确
2. 文件是否存在于 `subtitles` 目录
3. Web 服务器是否配置了正确的静态文件路径
