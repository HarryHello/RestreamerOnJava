package harryhelloo.restreamer.monitor;

import harryhelloo.restreamer.service.ObsService;
import io.obswebsocket.community.client.model.MediaState;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OBS 媒体源状态监测器
 * 
 * 功能：
 * 1. 定期监测指定 OBS 源的状态（播放/暂停/停止）
 * 2. 当检测到源停止或暂停时，自动重启
 * 3. 支持启用/禁用自动重启功能
 * 
 * 工作原理：
 * - 每隔指定时间检查媒体源状态
 * - 如果状态为 STOPPED 或 PAUSED，触发 RESTART 动作
 * - 如果源不在当前节目场景中，跳过检查（可选）
 * 
 * 注意：
 * - 该监测器无法区分用户手动操作和异常停止
 * - 如果需要在用户操作时不重启，请调用 stopMonitoring() 临时禁用
 * - 或者通过场景切换来自动禁用（当源不在当前场景时）
 */
@Log4j2
@Component
public class ObsMediaMonitor {

    /**
     * 监测任务执行器
     */
    private final Map<String, ScheduledExecutorService> monitorExecutors = new ConcurrentHashMap<>();

    /**
     * 上一次监测到的媒体状态（源名称 -> 状态）
     */
    private final Map<String, SourceMediaState> lastKnownStates = new ConcurrentHashMap<>();

    /**
     * 自动重启启用标志（源名称 -> 是否启用）
     */
    private final Map<String, AtomicBoolean> autoRestartEnabled = new ConcurrentHashMap<>();

    @Autowired
    private ObsService obsService;

    /**
     * 媒体状态枚举
     */
    public enum SourceMediaState {
        /** 播放中 */
        PLAYING,
        /** 暂停 */
        PAUSED,
        /** 停止 */
        STOPPED,
        /** 未知 */
        UNKNOWN
    }

    /**
     * 开始监测指定的 OBS 源
     *
     * @param sourceName 源名称（在 OBS 中显示的名称）
     * @param checkIntervalSeconds 检查间隔（秒）
     */
    public void startMonitoring(String sourceName, long checkIntervalSeconds) {
        if (monitorExecutors.containsKey(sourceName)) {
            log.info("Media monitor already started for source: {}", sourceName);
            return;
        }

        log.info("Starting media monitor for source: {} (interval: {}s)", sourceName, checkIntervalSeconds);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ObsMediaMonitor-" + sourceName);
            t.setDaemon(true);
            return t;
        });

        monitorExecutors.put(sourceName, executor);

        // 初始化状态标记
        lastKnownStates.put(sourceName, SourceMediaState.UNKNOWN);
        autoRestartEnabled.put(sourceName, new AtomicBoolean(true));  // 默认启用自动重启

        executor.scheduleWithFixedDelay(() -> {
            try {
                checkAndRecoverSource(sourceName);
            } catch (Exception e) {
                log.error("Error in media monitor for source: {}", sourceName, e);
            }
        }, checkIntervalSeconds, checkIntervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * 停止监测指定的 OBS 源
     *
     * @param sourceName 源名称
     */
    public void stopMonitoring(String sourceName) {
        ScheduledExecutorService executor = monitorExecutors.remove(sourceName);
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("Stopped media monitor for source: {}", sourceName);
        }
        lastKnownStates.remove(sourceName);
        autoRestartEnabled.remove(sourceName);
    }

    /**
     * 启用或禁用自动重启功能
     *
     * @param sourceName 源名称
     * @param enabled true=启用自动重启，false=禁用
     */
    public void setAutoRestartEnabled(String sourceName, boolean enabled) {
        AtomicBoolean flag = autoRestartEnabled.get(sourceName);
        if (flag != null) {
            flag.set(enabled);
            log.info("Auto-restart {} for source: {}", enabled ? "enabled" : "disabled", sourceName);
        }
    }

    /**
     * 检查自动重启是否启用
     */
    public boolean isAutoRestartEnabled(String sourceName) {
        AtomicBoolean flag = autoRestartEnabled.get(sourceName);
        return flag != null && flag.get();
    }

    /**
     * 检查源状态并在需要时恢复
     */
    private void checkAndRecoverSource(String sourceName) {
        if (!obsService.isReady()) {
            log.debug("OBS not ready, skipping media check for: {}", sourceName);
            return;
        }

        // 检查自动重启是否启用
        if (!isAutoRestartEnabled(sourceName)) {
            log.debug("Auto-restart disabled for source: {}", sourceName);
            // 即使不重启，也要更新状态
            SourceMediaState currentState = getCurrentMediaState(sourceName);
            lastKnownStates.put(sourceName, currentState);
            return;
        }

        // 获取当前媒体状态
        SourceMediaState currentState = getCurrentMediaState(sourceName);
        SourceMediaState previousState = lastKnownStates.getOrDefault(sourceName, SourceMediaState.UNKNOWN);

        log.debug("Source '{}' state: {} (previous: {})", sourceName, currentState, previousState);

        // 检查是否需要恢复
        boolean needRecovery = shouldRecover(sourceName, currentState, previousState);

        if (needRecovery) {
            log.info("Attempting to recover source '{}' (state: {}, previous: {})",
                    sourceName, currentState, previousState);
            recoverSource(sourceName);
        }

        // 更新状态
        lastKnownStates.put(sourceName, currentState);
    }

    /**
     * 获取当前媒体状态
     * 通过 OBS WebSocket API 获取媒体播放状态
     */
    private SourceMediaState getCurrentMediaState(String sourceName) {
        try {
            // 使用 CompletableFuture 获取状态
            var future = obsService.getMediaInputStatus(sourceName);
            if (future != null) {
                var response = future.get(5, TimeUnit.SECONDS);
                if (response != null && response.isSuccessful()) {
                    MediaState state = response.getMediaState();
                    if (state != null) {
                        // 将库的 MediaState 转换为我们的 SourceMediaState
                        switch (state) {
                            case PLAYING:
                                return SourceMediaState.PLAYING;
                            case PAUSED:
                                return SourceMediaState.PAUSED;
                            case STOPPED:
                                return SourceMediaState.STOPPED;
                            default:
                                return SourceMediaState.UNKNOWN;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get media status for '{}': {}", sourceName, e.getMessage());
        }
        return SourceMediaState.UNKNOWN;
    }

    /**
     * 判断是否需要恢复
     * 
     * 恢复条件：
     * 1. 当前状态为 STOPPED 或 PAUSED
     * 2. 之前状态为 PLAYING（说明是异常停止）
     */
    private boolean shouldRecover(String sourceName, SourceMediaState currentState, SourceMediaState previousState) {
        // 只在异常停止时恢复
        if (previousState == SourceMediaState.PLAYING) {
            if (currentState == SourceMediaState.STOPPED) {
                log.info("Detected abnormal stop for source '{}'", sourceName);
                return true;
            }
            if (currentState == SourceMediaState.PAUSED) {
                log.info("Detected abnormal pause for source '{}'", sourceName);
                return true;
            }
        }

        return false;
    }

    /**
     * 恢复媒体源（重启）
     */
    private void recoverSource(String sourceName) {
        try {
            // 使用 ObsService 的媒体重启功能
            // 先触发 RESTART 动作
            var future = obsService.triggerMediaAction(sourceName, harryhelloo.restreamer.util.MediaInputActions.RESTART);
            if (future != null) {
                future.get(10, TimeUnit.SECONDS);
                log.info("Successfully triggered RESTART for source: {}", sourceName);
            }
        } catch (Exception e) {
            log.error("Failed to recover source '{}': {}", sourceName, e.getMessage());
        }
    }

    /**
     * 检查是否正在监测指定源
     */
    public boolean isMonitoring(String sourceName) {
        return monitorExecutors.containsKey(sourceName);
    }
}
