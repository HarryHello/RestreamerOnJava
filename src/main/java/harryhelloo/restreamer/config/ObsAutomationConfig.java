package harryhelloo.restreamer.config;

import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.monitor.ChannelStatusMonitor;
import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.service.ObsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * OBS 自动化配置
 * 
 * <p>根据直播状态自动控制 OBS 推流和录制。</p>
 * 
 * <h2>自动化功能：</h2>
 * <ul>
 *     <li>直播开始时自动启动 OBS 推流</li>
 *     <li>直播开始时根据配置自动开始录制</li>
 *     <li>直播结束时自动停止 OBS 推流</li>
 *     <li>直播结束时根据配置自动停止录制</li>
 * </ul>
 * 
 * <h2>配置项：</h2>
 * <ul>
 *     <li>{@code doObsRecord} - 是否启用 OBS 录制</li>
 *     <li>{@code doAutoRestartObsSource} - 是否自动重启 OBS 媒体源</li>
 * </ul>
 * 
 * @author harryhelloo
 * @version 1.0
 */
@Log4j2
@Configuration
public class ObsAutomationConfig {

    @Autowired
    private ChannelStatusMonitor channelStatusMonitor;

    @Autowired
    private ObsService obsService;

    /**
     * 为指定频道配置 OBS 自动化处理
     * 
     * @param channelId 频道 ID
     */
    public void configureAutomationForChannel(String channelId) {
        log.info("Configuring OBS automation for channel: {}", channelId);

        // 注册直播开始回调
        channelStatusMonitor.onStreaming(channelId, (id) -> {
            log.info("Stream started detected for channel: {}, triggering OBS automation", id);
            
            Settings settings = SettingsManager.getInstance().getSettings();
            
            try {
                // 根据配置决定是否录制
                if (Boolean.TRUE.equals(settings.getDoObsRecord())) {
                    log.info("Auto-starting OBS recording for channel: {}", id);
                    obsService.startRecord()
                        .thenAccept(response -> log.info("OBS recording started successfully for channel: {}", id))
                        .exceptionally(e -> {
                            log.error("Failed to start OBS recording for channel: {}", id, e);
                            return null;
                        });
                } else {
                    log.debug("OBS recording is disabled for channel: {}", id);
                }

                // 启动推流
                log.info("Auto-starting OBS stream for channel: {}", id);
                obsService.startStream()
                    .thenAccept(response -> log.info("OBS stream started successfully for channel: {}", id))
                    .exceptionally(e -> {
                        log.error("Failed to start OBS stream for channel: {}", id, e);
                        return null;
                    });

            } catch (Exception e) {
                log.error("Error in OBS automation (onStreaming) for channel: {}", id, e);
            }
        });

        // 注册直播结束回调
        channelStatusMonitor.onEndStream(channelId, (id) -> {
            log.info("Stream ended detected for channel: {}, triggering OBS automation", id);
            
            Settings settings = SettingsManager.getInstance().getSettings();
            
            try {
                // 根据配置决定是否停止录制
                if (Boolean.TRUE.equals(settings.getDoObsRecord())) {
                    log.info("Auto-stopping OBS recording for channel: {}", id);
                    obsService.stopRecord()
                        .thenAccept(response -> log.info("OBS recording stopped successfully for channel: {}", id))
                        .exceptionally(e -> {
                            log.error("Failed to stop OBS recording for channel: {}", id, e);
                            return null;
                        });
                } else {
                    log.debug("OBS recording is disabled for channel: {}", id);
                }

                // 停止推流
                log.info("Auto-stopping OBS stream for channel: {}", id);
                obsService.stopStream()
                    .thenAccept(response -> log.info("OBS stream stopped successfully for channel: {}", id))
                    .exceptionally(e -> {
                        log.error("Failed to stop OBS stream for channel: {}", id, e);
                        return null;
                    });

            } catch (Exception e) {
                log.error("Error in OBS automation (onEndStream) for channel: {}", id, e);
            }
        });

        log.info("OBS automation configured successfully for channel: {}", channelId);
    }
}
