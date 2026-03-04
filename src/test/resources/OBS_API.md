# OBS API 接口文档

## 基础 OBS 控制

### 1. 开始直播

```http
PUT /api/obs/stream/start
```

**响应：**
- `200 OK`: "success"
- `500 Internal Server Error`: 失败信息

### 2. 停止直播

```http
PUT /api/obs/stream/stop
```

**响应：**
- `200 OK`: "success"
- `500 Internal Server Error`: 失败信息

### 3. 开始录制

```http
PUT /api/obs/record/start
```

**响应：**
- `200 OK`: "success"
- `500 Internal Server Error`: 失败信息

### 4. 停止录制

```http
PUT /api/obs/record/stop
```

**响应：**
- `200 OK`: "success"
- `500 Internal Server Error`: 失败信息

### 5. 切换场景

```http
PUT /api/obs/scene/set?name=场景名称
```

**参数：**
- `name` (可选): 场景名称，默认 "转播"

**响应：**
- `200 OK`: "success"
- `500 Internal Server Error`: 失败信息

---

## 媒体监测 API

媒体监测功能使用 `Settings` 中的配置：
- `doAutoRestartObsSource`: 是否启用自动重启（默认 `true`）
- `ObsSource`: 媒体源名称（默认 `"stream"`）
- `ObsScene`: 场景名称（默认 `"restreamer"`）

### 6. 启动媒体源监测

```http
POST /api/obs/media-monitor/start
Content-Type: application/json

{
  "sourceName": "stream",
  "interval": 10
}
```

**请求体（可选）：**
- `sourceName` (可选): OBS 中源的名称，如果不提供则使用 Settings 中的 `ObsSource`
- `interval` (可选): 检查间隔（秒），默认 10

**响应：**
- `200 OK`: "Media monitor started for source: stream"
- `400 Bad Request`: "sourceName is required or not configured in Settings"
- `500 Internal Server Error`: 失败信息

### 7. 停止媒体源监测

```http
POST /api/obs/media-monitor/stop
Content-Type: application/json

{
  "sourceName": "stream"
}
```

**请求体（可选）：**
- `sourceName` (可选): OBS 中源的名称，如果不提供则使用 Settings 中的 `ObsSource`

**响应：**
- `200 OK`: "Media monitor stopped for source: stream"
- `400 Bad Request`: "sourceName is required or not configured in Settings"
- `500 Internal Server Error`: 失败信息

### 8. 获取媒体监测状态

```http
GET /api/obs/media-monitor/status?sourceName=stream
```

**参数：**
- `sourceName` (可选): OBS 中源的名称，如果不提供则使用 Settings 中的 `ObsSource`

**响应示例：**
```json
{
  "sourceName": "stream",
  "isMonitoring": true,
  "isAutoRestartEnabled": true
}
```

**响应：**
- `200 OK`: 状态信息
- `500 Internal Server Error`: 失败信息

---

## 使用示例

### cURL 示例

#### 启动媒体监测（使用 Settings 默认配置）
```bash
curl -X POST http://localhost:8080/api/obs/media-monitor/start \
  -H "Content-Type: application/json" \
  -d '{}'
```

#### 启动媒体监测（指定源和间隔）
```bash
curl -X POST http://localhost:8080/api/obs/media-monitor/start \
  -H "Content-Type: application/json" \
  -d '{"sourceName": "stream", "interval": 10}'
```

#### 停止媒体监测（使用 Settings 默认配置）
```bash
curl -X POST http://localhost:8080/api/obs/media-monitor/stop \
  -H "Content-Type: application/json" \
  -d '{}'
```

#### 获取监测状态（使用 Settings 默认配置）
```bash
curl -X GET "http://localhost:8080/api/obs/media-monitor/status"
```

#### 获取监测状态（指定源）
```bash
curl -X GET "http://localhost:8080/api/obs/media-monitor/status?sourceName=stream"
```

### JavaScript/Fetch 示例

```javascript
// 启动媒体监测（使用 Settings 默认配置）
async function startMediaMonitor() {
  const response = await fetch('/api/obs/media-monitor/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({})
  });
  return await response.text();
}

// 启动媒体监测（自定义配置）
async function startMediaMonitorCustom(sourceName, interval = 10) {
  const response = await fetch('/api/obs/media-monitor/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sourceName, interval })
  });
  return await response.text();
}

// 停止媒体监测
async function stopMediaMonitor() {
  const response = await fetch('/api/obs/media-monitor/stop', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({})
  });
  return await response.text();
}

// 获取监测状态
async function getMediaMonitorStatus(sourceName) {
  const url = sourceName 
    ? `/api/obs/media-monitor/status?sourceName=${sourceName}`
    : '/api/obs/media-monitor/status';
    
  const response = await fetch(url);
  return await response.json();
}

// 使用示例
await startMediaMonitor();  // 使用 Settings 默认配置

// 获取状态
const status = await getMediaMonitorStatus();
console.log('Monitoring:', status.isMonitoring);
console.log('Auto-restart:', status.isAutoRestartEnabled);
```

---

## 自动启动行为

应用启动时，`ObsMediaMonitorInitializer` 会根据 `Settings` 中的配置自动启动媒体监测：

**条件：**
- `doAutoRestartObsSource = true`（默认值）
- `ObsSource` 不为空（默认值："stream"）

**行为：**
- 自动以 10 秒间隔启动监测
- 如果检测到源停止/暂停，自动触发重启

### 通过 Settings 配置

更新 `Settings` 来配置媒体监测行为：

```bash
# 更新 Settings
curl -X PUT http://localhost:8080/api/settings \
  -H "Content-Type: application/json" \
  -d '{
    "doAutoRestartObsSource": true,
    "ObsScene": "restreamer",
    "ObsSource": "stream"
  }'
```

### 禁用自动重启

如果需要临时禁用自动重启功能，有两种方式：

**方式 1：更新 Settings**
```bash
curl -X POST http://localhost:8080/api/settings/doAutoRestartObsSource \
  -H "Content-Type: application/json" \
  -d 'false'
```
然后重启应用。

**方式 2：停止监测**
```bash
curl -X POST http://localhost:8080/api/obs/media-monitor/stop \
  -H "Content-Type: application/json" \
  -d '{}'
```
