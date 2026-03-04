# 翻译与字幕集成 API 文档

## 概述

翻译服务已与字幕记录功能集成，在翻译的同时可以自动记录原文和翻译字幕。

## API 接口

### 1. 获取可用翻译模型

```http
GET /api/translation/models
```

**响应示例：**
```json
{
  "options": [
    { "label": "GPT-4", "value": "gpt-4" },
    { "label": "GPT-3.5-turbo", "value": "gpt-3.5-turbo" }
  ]
}
```

### 2. 流式翻译（带字幕记录）

```http
POST /api/translation/translate/stream?channelId=xxx&recordSubtitle=true
Content-Type: application/json

{
  "text": "Hello World",
  "from": "en",
  "to": "zh"
}
```

**参数：**
- `channelId` (可选): 频道 ID，用于字幕记录
- `recordSubtitle` (可选): 是否记录字幕，默认 `false`

**SSE 事件流：**
```
event: translation
data: 你好

event: translation
data: 世界

event: complete
data: {"status":"completed"}
```

**字幕记录：**
- 如果 `channelId` 和 `recordSubtitle=true`，会自动记录原文和翻译字幕
- 字幕文件保存在 `subtitles/` 目录
- 原文和翻译的时间戳保持同步

### 3. 单次翻译（不带字幕）

```http
POST /api/translation/translate
Content-Type: application/json

{
  "text": "Hello World",
  "from": "en",
  "to": "zh"
}
```

**响应：** 翻译结果（纯文本）

### 4. 开始翻译会话（带字幕录制）

```http
POST /api/translation/session/start
Content-Type: application/json

{
  "channelId": "UCxsZ6NCzjU_t4YSxQLBcM5A",
  "channelName": "Monstercat",
  "streamLink": "https://www.twitch.tv/monstercat"
}
```

**说明：**
- 启动字幕录制会话
- 后续的翻译会自动记录字幕
- 字幕文件名：`[时间]_source_[channelName]_[streamLink].srt`

### 5. 结束翻译会话

```http
POST /api/translation/session/end
Content-Type: application/json

{
  "channelId": "UCxsZ6NCzjU_t4YSxQLBcM5A"
}
```

**说明：**
- 停止字幕录制
- 关闭 SRT 文件写入器

## 使用示例

### JavaScript/Fetch 示例

#### 流式翻译并记录字幕

```javascript
async function translateWithSubtitle(text, channelId) {
  const response = await fetch(
    `/api/translation/translate/stream?channelId=${channelId}&recordSubtitle=true`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        text: text,
        from: 'en',
        to: 'zh'
      })
    }
  );

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let translatedText = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const chunk = decoder.decode(value);
    console.log('Received chunk:', chunk);
    
    // 解析 SSE 事件
    const lines = chunk.split('\n');
    for (const line of lines) {
      if (line.startsWith('data: ')) {
        translatedText += line.substring(6);
      }
    }
  }

  return translatedText;
}

// 使用示例
const channelId = 'UCxsZ6NCzjU_t4YSxQLBcM5A';
const result = await translateWithSubtitle('Hello World', channelId);
console.log('Translation:', result);
console.log('Subtitle saved to subtitles/ directory');
```

#### 管理翻译会话

```javascript
// 开始会话
async function startTranslationSession(channelId, channelName, streamLink) {
  const response = await fetch('/api/translation/session/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ channelId, channelName, streamLink })
  });
  return await response.text();
}

// 结束会话
async function endTranslationSession(channelId) {
  const response = await fetch('/api/translation/session/end', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ channelId })
  });
  return await response.text();
}

// 完整流程
async function liveTranslation() {
  const channelId = 'UCxsZ6NCzjU_t4YSxQLBcM5A';
  const channelName = 'Monstercat';
  const streamLink = 'https://www.twitch.tv/monstercat';
  
  // 开始会话
  await startTranslationSession(channelId, channelName, streamLink);
  
  // 进行多次翻译
  const texts = ['Hello', 'World', 'Welcome to the live stream'];
  for (const text of texts) {
    await translateWithSubtitle(text, channelId);
  }
  
  // 结束会话
  await endTranslationSession(channelId);
  
  console.log('Translation session completed');
  console.log('Subtitles saved to subtitles/ directory');
}
```

### Vue.js 示例

```vue
<template>
  <div>
    <textarea v-model="inputText" placeholder="输入要翻译的文本"></textarea>
    <button @click="translate" :disabled="translating">
      {{ translating ? '翻译中...' : '翻译' }}
    </button>
    
    <div v-if="translatedText">
      <h3>翻译结果：</h3>
      <p>{{ translatedText }}</p>
    </div>
    
    <div>
      <label>
        <input type="checkbox" v-model="recordSubtitle">
        记录字幕
      </label>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      inputText: '',
      translatedText: '',
      translating: false,
      recordSubtitle: false,
      channelId: 'UCxsZ6NCzjU_t4YSxQLBcM5A'
    }
  },
  methods: {
    async translate() {
      if (!this.inputText) return
      
      this.translating = true
      this.translatedText = ''
      
      try {
        const params = new URLSearchParams({
          channelId: this.recordSubtitle ? this.channelId : '',
          recordSubtitle: this.recordSubtitle
        })
        
        const response = await fetch(`/api/translation/translate/stream?${params}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            text: this.inputText,
            from: 'en',
            to: 'zh'
          })
        })
        
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          
          const chunk = decoder.decode(value)
          const lines = chunk.split('\n')
          
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              this.translatedText += line.substring(6)
            }
          }
        }
        
        if (this.recordSubtitle) {
          console.log('字幕已记录到 subtitles/ 目录')
        }
        
      } catch (error) {
        console.error('Translation error:', error)
      } finally {
        this.translating = false
      }
    }
  }
}
</script>
```

## 工作流程

```
用户请求翻译
    ↓
检查是否启用字幕记录
    ↓
┌─ 是 ─┐          ┌─ 否 ─┐
│      │          │      │
▼      │          ▼      │
调用 subtitleService   │
    │      │          │      │
startRecording()     │      │
    │      │          │      │
▼      │          ▼      │
流式翻译          流式翻译
    │      │          │      │
▼      │          ▼      │
逐块返回结果    逐块返回结果
    │      │          │      │
▼      │          │      │
累积翻译结果    │      │
    │      │          │      │
▼      │          │      │
翻译完成          │      │
    │      │          │      │
▼      │          │      │
调用 subtitleService   │      │
    │      │          │      │
writeBoth()      │      │
    │      │          │      │
▼      │          │      │
保存 SRT 文件    │      │
    │      │          │      │
▼      │          ▼      │
完成响应          完成响应
```

## 字幕文件示例

**原文字幕：**
```srt
1
00:00:01,000 --> 00:00:03,000
Hello, welcome to our live stream

2
00:00:03,500 --> 00:00:06,000
Today we have a special guest
```

**翻译字幕：**
```srt
1
00:00:01,000 --> 00:00:03,000
你好，欢迎观看我们的直播

2
00:00:03,500 --> 00:00:06,000
今天我们有一位特别嘉宾
```

## 注意事项

1. **字幕记录是可选的** - 通过 `recordSubtitle` 参数控制
2. **时间戳同步** - 原文和翻译的时间戳完全一致
3. **文件命名** - 自动使用频道名称和直播链接
4. **会话管理** - 使用 session API 可以管理长时间的直播
5. **安全清理** - 客户端断开连接时会自动清理资源

## 错误处理

### 字幕记录失败

如果字幕记录失败（如磁盘空间不足），翻译仍然会正常进行，只会在日志中记录错误。

### 会话超时

如果会话长时间没有活动，会自动清理资源。建议在前端页面关闭时调用 `/api/translation/session/end`。
