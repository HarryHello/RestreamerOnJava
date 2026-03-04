package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.monitor.ObsMediaMonitor;
import harryhelloo.restreamer.service.ObsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OBS 控制控制器
 *
 * <p>提供 OBS Studio 的远程控制功能，包括推流、录制、场景切换和媒体源监控。</p>
 *
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>推流控制（开始/停止）</li>
 *     <li>录制控制（开始/停止）</li>
 *     <li>场景切换</li>
 *     <li>OBS 媒体源自动监控（自动重启停止的媒体源）</li>
 * </ul>
 *
 * <h2>API 端点：</h2>
 * <h3>推流控制：</h3>
 * <ul>
 *     <li>{@code PUT /api/obs/stream/start} - 开始推流</li>
 *     <li>{@code PUT /api/obs/stream/stop} - 停止推流</li>
 * </ul>
 *
 * <h3>录制控制：</h3>
 * <ul>
 *     <li>{@code PUT /api/obs/record/start} - 开始录制</li>
 *     <li>{@code PUT /api/obs/record/stop} - 停止录制</li>
 * </ul>
 *
 * <h3>场景控制：</h3>
 * <ul>
 *     <li>{@code PUT /api/obs/scene/set} - 切换场景</li>
 * </ul>
 *
 * <h3>媒体源监控：</h3>
 * <ul>
 *     <li>{@code POST /api/obs/media-monitor/start} - 启动媒体源监测</li>
 *     <li>{@code POST /api/obs/media-monitor/stop} - 停止媒体源监测</li>
 *     <li>{@code GET /api/obs/media-monitor/status} - 获取监测状态</li>
 * </ul>
 *
 * @author harryhelloo
 * @version 1.0
 * @see ObsService
 * @see ObsMediaMonitor
 */
@Log4j2
@RestController
@RequestMapping("/api/obs")
public class ObsController {
    @Autowired
    private ObsService obsService;

    @Autowired
    private ObsMediaMonitor mediaMonitor;

    /**
     * 开始推流
     *
     * @return 操作结果的 CompletableFuture
     */
    @PutMapping("/stream/start")
    public CompletableFuture<ResponseEntity<String>> startStream() {
        return obsService.startStream()
            .thenApply(response -> ResponseEntity.ok("success"))
            .exceptionally(e -> ResponseEntity.internalServerError().body("Failed to start stream. %s".formatted(e.getMessage())));
    }

    /**
     * 停止推流
     *
     * @return 操作结果的 CompletableFuture
     */
    @PutMapping("/stream/stop")
    public CompletableFuture<ResponseEntity<String>> stopStream() {
        return obsService.stopStream()
            .thenApply(response -> ResponseEntity.ok("success"))
            .exceptionally(e -> ResponseEntity.internalServerError().body("Failed to stop stream. %s".formatted(e.getMessage())));
    }

    /**
     * 开始录制
     *
     * @return 操作结果的 CompletableFuture
     */
    @PutMapping("/record/start")
    public CompletableFuture<ResponseEntity<String>> startRecord() {
        return obsService.startRecord()
            .thenApply(response -> ResponseEntity.ok("success"))
            .exceptionally(e -> ResponseEntity.internalServerError().body("Failed to start record. %s".formatted(e.getMessage())));
    }

    /**
     * 停止录制
     *
     * @return 操作结果的 CompletableFuture
     */
    @PutMapping("/record/stop")
    public CompletableFuture<ResponseEntity<String>> stopRecord() {
        return obsService.stopRecord()
            .thenApply(response -> ResponseEntity.ok("success"))
            .exceptionally(e -> ResponseEntity.internalServerError().body("Failed to stop record. %s".formatted(e.getMessage())));
    }

    /**
     * 切换 OBS 场景
     *
     * @param name 场景名称（默认 "restreamer"）
     * @return 操作结果的 CompletableFuture
     */
    @PutMapping("/scene/set")
    public CompletableFuture<ResponseEntity<String>> setScene(
        @RequestParam(value = "name", defaultValue = "restreamer") String name
    ) {
        return obsService.setScene(name)
            .thenApply(response -> ResponseEntity.ok("success"))
            .exceptionally(e -> ResponseEntity.internalServerError().body("Failed to  set scene. %s".formatted(e.getMessage())));
    }

    // ==================== 媒体监测相关 API ====================

    /**
     * 启动媒体源监测
     * 使用 Settings 中的配置（ObsSource, doAutoRestartObsSource）
     */
    @PostMapping("/media-monitor/start")
    public ResponseEntity<String> startMediaMonitor(@RequestBody(required = false) Map<String, Object> params) {
        try {
            // 如果提供了参数，使用参数；否则使用 Settings 默认值
            String sourceName = null;
            long checkInterval = 10;

            if (params != null) {
                sourceName = (String) params.get("sourceName");
                Number interval = (Number) params.get("interval");
                if (interval != null) {
                    checkInterval = interval.longValue();
                }
            }

            // 如果未提供 sourceName，使用 Settings 中的默认值
            if (sourceName == null || sourceName.trim().isEmpty()) {
                sourceName = SettingsManager.getInstance().getSettings().getObsSource();
            }

            if (sourceName == null || sourceName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("sourceName is required or not configured in Settings");
            }

            mediaMonitor.startMonitoring(sourceName.trim(), checkInterval);

            return ResponseEntity.ok("Media monitor started for source: " + sourceName);
        } catch (Exception e) {
            log.error("Failed to start media monitor", e);
            return ResponseEntity.internalServerError().body("Failed to start media monitor: " + e.getMessage());
        }
    }

    /**
     * 停止媒体源监测
     */
    @PostMapping("/media-monitor/stop")
    public ResponseEntity<String> stopMediaMonitor(@RequestBody(required = false) Map<String, String> params) {
        try {
            // 如果提供了参数，使用参数；否则使用 Settings 中的默认值
            String sourceName = null;

            if (params != null && params.containsKey("sourceName")) {
                sourceName = params.get("sourceName");
            } else {
                sourceName = SettingsManager.getInstance().getSettings().getObsSource();
            }

            if (sourceName == null || sourceName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("sourceName is required or not configured in Settings");
            }

            mediaMonitor.stopMonitoring(sourceName.trim());
            return ResponseEntity.ok("Media monitor stopped for source: " + sourceName);
        } catch (Exception e) {
            log.error("Failed to stop media monitor", e);
            return ResponseEntity.internalServerError().body("Failed to stop media monitor: " + e.getMessage());
        }
    }

    /**
     * 获取媒体监测状态
     */
    @GetMapping("/media-monitor/status")
    public ResponseEntity<Map<String, Object>> getMediaMonitorStatus(
        @RequestParam(value = "sourceName", required = false) String sourceName
    ) {
        try {
            java.util.Map<String, Object> status = new java.util.HashMap<>();

            // 如果未提供 sourceName，使用 Settings 中的默认值
            if (sourceName == null || sourceName.trim().isEmpty()) {
                sourceName = SettingsManager.getInstance().getSettings().getObsSource();
            }

            if (sourceName != null && !sourceName.trim().isEmpty()) {
                boolean isMonitoring = mediaMonitor.isMonitoring(sourceName.trim());
                boolean isAutoRestartEnabled = isMonitoring ? mediaMonitor.isAutoRestartEnabled(sourceName.trim()) : false;

                status.put("sourceName", sourceName);
                status.put("isMonitoring", isMonitoring);
                status.put("isAutoRestartEnabled", isAutoRestartEnabled);
            } else {
                status.put("message", "No source configured. Set ObsSource in Settings or provide sourceName parameter.");
            }

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get media monitor status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
