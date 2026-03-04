package harryhelloo.restreamer.monitor;

import harryhelloo.restreamer.pojo.StreamerChannel;
import harryhelloo.restreamer.service.ChannelService;
import harryhelloo.restreamer.service.SubtitleService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 频道状态监控器
 *
 * <p>定期监测频道直播状态，当检测到直播开始时自动通知字幕服务。</p>
 *
 * <h2>回调接口：</h2>
 * <ul>
 *     <li>{@link #onStreaming(String, Consumer)} - 当直播开始时触发</li>
 *     <li>{@link #onEndStream(String, Consumer)} - 当直播结束时触发</li>
 * </ul>
 *
 * @author harryhelloo
 * @version 1.0
 */
@Log4j2
@Component
public class ChannelStatusMonitor {

    private final Map<String, ScheduledExecutorService> monitorExecutors = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    /**
     * 记录上次检测的直播状态，用于检测状态变化
     */
    private final Map<String, Boolean> lastKnownStreamingStatus = new ConcurrentHashMap<>();
    /**
     * 直播开始回调函数（channelId -> 回调）
     * 可用于自动化处理，无需前端调用
     */
    private final Map<String, Consumer<String>> onStreamingCallbacks = new ConcurrentHashMap<>();
    /**
     * 直播结束回调函数（channelId -> 回调）
     * 可用于自动化处理，无需前端调用
     */
    private final Map<String, Consumer<String>> onEndStreamCallbacks = new ConcurrentHashMap<>();
    @Autowired
    private ChannelService channelService;
    @Autowired
    private SubtitleService subtitleService;

    /**
     * 注册直播开始回调
     *
     * @param channelId 频道 ID
     * @param callback  回调函数，参数为 channelId
     */
    public void onStreaming(String channelId, Consumer<String> callback) {
        onStreamingCallbacks.put(channelId, callback);
        log.info("Registered onStreaming callback for channel: {}", channelId);
    }

    /**
     * 注册直播结束回调
     *
     * @param channelId 频道 ID
     * @param callback  回调函数，参数为 channelId
     */
    public void onEndStream(String channelId, Consumer<String> callback) {
        onEndStreamCallbacks.put(channelId, callback);
        log.info("Registered onEndStream callback for channel: {}", channelId);
    }

    /**
     * 移除直播开始回调
     *
     * @param channelId 频道 ID
     */
    public void removeOnStreaming(String channelId) {
        onStreamingCallbacks.remove(channelId);
    }

    /**
     * 移除直播结束回调
     *
     * @param channelId 频道 ID
     */
    public void removeOnEndStream(String channelId) {
        onEndStreamCallbacks.remove(channelId);
    }

    public void startMonitoring(StreamerChannel streamerChannel) {
        String channelId = streamerChannel.getChannelId();
        if (monitorExecutors.containsKey(channelId)) {
            log.info("Monitoring already started for channel: {}", channelId);
            return;
        }

        // 初始化状态记录
        lastKnownStreamingStatus.put(channelId, false);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        monitorExecutors.put(channelId, executor);

        executor.scheduleWithFixedDelay(() -> {
            try {
                // 开始检查时，先发送 isCheckingStream=true 的状态
                streamerChannel.setCheckingStream(true);
                channelService.updateChannel(streamerChannel);
                sendStatusUpdate(streamerChannel);

                channelService.getChannelStatus(streamerChannel);

                // 检测直播状态变化
                boolean isStreaming = streamerChannel.isStreaming();
                Boolean wasStreaming = lastKnownStreamingStatus.get(channelId);

                // 当检测到直播开始时
                if (isStreaming && (wasStreaming == null || !wasStreaming)) {
                    log.info("Stream started detected for channel: {}, setting stream start time", channelId);
                    subtitleService.setStreamStartTime(channelId);

                    // 触发回调
                    Consumer<String> callback = onStreamingCallbacks.get(channelId);
                    if (callback != null) {
                        log.info("Triggering onStreaming callback for channel: {}", channelId);
                        callback.accept(channelId);
                    }
                }

                // 当检测到直播结束时
                if (!isStreaming && (wasStreaming != null && wasStreaming)) {
                    log.info("Stream ended detected for channel: {}", channelId);

                    // 触发回调
                    Consumer<String> callback = onEndStreamCallbacks.get(channelId);
                    if (callback != null) {
                        log.info("Triggering onEndStream callback for channel: {}", channelId);
                        callback.accept(channelId);
                    }
                }

                // 更新状态记录
                lastKnownStreamingStatus.put(channelId, isStreaming);

                // 根据不同状态调整检测间隔
                int interval = getIntervalByStatus(streamerChannel);
                rescheduleMonitoring(streamerChannel, interval);
            } catch (Exception e) {
                log.error("Error monitoring channel: {}", channelId, e);
                channelService.setUnknownStatus(streamerChannel);
                sendStatusUpdate(streamerChannel);
            }
        }, 0, 30, TimeUnit.SECONDS);

        log.info("Started monitoring channel: {}", channelId);
    }

    public void stopMonitoring(StreamerChannel streamerChannel) {
        var channelId = streamerChannel.getChannelId();
        try (ScheduledExecutorService executor = monitorExecutors.remove(channelId)) {
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
            }
        }

        SseEmitter emitter = sseEmitters.remove(channelId);
        if (emitter != null) {
            emitter.complete();
        }

        // 清理状态记录和回调
        lastKnownStreamingStatus.remove(channelId);
        onStreamingCallbacks.remove(channelId);
        onEndStreamCallbacks.remove(channelId);

        log.info("Stopped monitoring channel: {}", channelId);
    }

    public SseEmitter registerSseEmitter(StreamerChannel streamerChannel) {
        var channelId = streamerChannel.getChannelId();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitters.put(channelId, emitter);

        emitter.onCompletion(() -> {
            sseEmitters.remove(channelId);
            log.info("SSE emitter completed for channel: {}", channelId);
        });

        emitter.onError(e -> {
            sseEmitters.remove(channelId);
            log.error("SSE emitter error for channel: {}", channelId, e);
        });

        // 立即发送当前状态
        StreamerChannel currentChannel = channelService.getChannel(channelId);
        if (currentChannel != null) {
            sendStatusUpdate(currentChannel);
        }

        return emitter;
    }

    private void sendStatusUpdate(StreamerChannel channel) {
        String channelId = channel.getChannelId();
        SseEmitter emitter = sseEmitters.get(channelId);
        if (emitter != null) {
            try {
                emitter.send(channel);
            } catch (IOException e) {
                log.error("Error sending SSE update for channel: {}", channelId, e);
                sseEmitters.remove(channelId);
                emitter.completeWithError(e);
            }
        }
    }

    private int getIntervalByStatus(StreamerChannel channel) {
        if (channel.isStreaming()) {
            return 30; // 正在直播，30 秒检测一次
        } else if (channel.isUpcomingStream()) {
            return 30; // 即将直播，30 秒检测一次
        } else {
            return 300; // 无直播，5 分钟检测一次
        }
    }

    private void rescheduleMonitoring(StreamerChannel channel, int interval) {
        var channelId = channel.getChannelId();
        ScheduledExecutorService executor = monitorExecutors.get(channelId);
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

            ScheduledExecutorService newExecutor = Executors.newSingleThreadScheduledExecutor();
            monitorExecutors.put(channelId, newExecutor);

            newExecutor.scheduleWithFixedDelay(() -> {
                try {
                    channel.setCheckingStream(true);
                    channelService.updateChannel(channel);
                    sendStatusUpdate(channel);

                    channelService.getChannelStatus(channel);

                    // 检查结束后，发送完整状态（isCheckingStream=false）
                    channel.setCheckingStream(false);
                    sendStatusUpdate(channel);

                    int newInterval = getIntervalByStatus(channel);
                    if (newInterval != interval) {
                        rescheduleMonitoring(channel, newInterval);
                    }
                } catch (Exception e) {
                    log.error("Error monitoring channel: {}", channel, e);
                    channel.setCheckingStream(false);
                    sendStatusUpdate(channel);
                }
            }, interval, interval, TimeUnit.SECONDS);

            log.info("Rescheduled monitoring for channel: {} with interval: {} seconds", channel, interval);
        }
    }

    public boolean isMonitoring(String channelId) {
        return monitorExecutors.containsKey(channelId);
    }

    public StreamerChannel getCurrentStatus(String channelId) {
        return channelService.getChannel(channelId);
    }
}
