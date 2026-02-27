package harryhelloo.restreamer.service;

import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.pojo.youtube.Channel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class ChannelStatusMonitor {

    @Autowired
    private ChannelService channelService;

    private final Map<String, ScheduledExecutorService> monitorExecutors = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    private final Map<String, Channel> channelStates = new ConcurrentHashMap<>();

    public void startMonitoring(String channelId) {
        if (monitorExecutors.containsKey(channelId)) {
            log.info("Monitoring already started for channel: {}", channelId);
            return;
        }

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        monitorExecutors.put(channelId, executor);

        executor.scheduleWithFixedDelay(() -> {
            try {
                Channel channel = channelService.getChannelStatus(channelId);
                channelStates.put(channelId, channel);
                sendStatusUpdate(channelId, channel);

                // 根据不同状态调整检测间隔
                int interval = getIntervalByStatus(channel);
                rescheduleMonitoring(channelId, interval);
            } catch (Exception e) {
                log.error("Error monitoring channel: {}", channelId, e);
            }
        }, 0, 30, TimeUnit.SECONDS);

        log.info("Started monitoring channel: {}", channelId);
    }

    public void stopMonitoring(String channelId) {
        ScheduledExecutorService executor = monitorExecutors.remove(channelId);
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

        SseEmitter emitter = sseEmitters.remove(channelId);
        if (emitter != null) {
            emitter.complete();
        }

        channelStates.remove(channelId);
        log.info("Stopped monitoring channel: {}", channelId);
    }

    public SseEmitter registerSseEmitter(String channelId) {
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
        Channel currentChannel = channelStates.get(channelId);
        if (currentChannel != null) {
            sendStatusUpdate(channelId, currentChannel);
        }

        return emitter;
    }

    private void sendStatusUpdate(String channelId, Channel channel) {
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

    private int getIntervalByStatus(Channel channel) {
        if (channel.isStreaming()) {
            return 30; // 正在直播，15秒检测一次
        } else if (channel.isUpcomingStream()) {
            return 30; // 即将直播，60秒检测一次
        } else {
            return 300; // 无直播，5分钟检测一次
        }
    }

    private void rescheduleMonitoring(String channelId, int interval) {
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
                    Channel channel = channelService.getChannelStatus(channelId);
                    channelStates.put(channelId, channel);
                    sendStatusUpdate(channelId, channel);

                    int newInterval = getIntervalByStatus(channel);
                    if (newInterval != interval) {
                        rescheduleMonitoring(channelId, newInterval);
                    }
                } catch (Exception e) {
                    log.error("Error monitoring channel: {}", channelId, e);
                }
            }, interval, interval, TimeUnit.SECONDS);

            log.info("Rescheduled monitoring for channel: {} with interval: {} seconds", channelId, interval);
        }
    }

    public boolean isMonitoring(String channelId) {
        return monitorExecutors.containsKey(channelId);
    }

    public Channel getCurrentStatus(String channelId) {
        return channelStates.get(channelId);
    }
}