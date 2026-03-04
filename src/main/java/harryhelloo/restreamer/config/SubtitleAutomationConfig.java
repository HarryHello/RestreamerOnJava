package harryhelloo.restreamer.config;

import harryhelloo.restreamer.monitor.ChannelStatusMonitor;
import harryhelloo.restreamer.service.SubtitleService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 字幕自动化配置
 * 
 * <p>配置直播状态变化的自动处理逻辑，无需前端调用。</p>
 * 
 * <h2>自动化功能：</h2>
 * <ul>
 *     <li>直播开始时自动设置直播开始时间（用于字幕时间计算）</li>
 *     <li>直播结束时自动停止字幕录制</li>
 * </ul>
 * 
 * <h2>工作流程：</h2>
 * <ol>
 *     <li>前端调用 {@code /api/channel/monitor/start} 启动监控</li>
 *     <li>后端自动配置回调（通过 {@link #configureAutomationForChannel(String)}）</li>
 *     <li>{@link ChannelStatusMonitor} 检测到直播状态变化</li>
 *     <li>自动触发相应的回调函数</li>
 * </ol>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see ChannelStatusMonitor
 * @see SubtitleService
 */
@Log4j2
@Configuration
public class SubtitleAutomationConfig {

    @Autowired
    private ChannelStatusMonitor channelStatusMonitor;

    @Autowired
    private SubtitleService subtitleService;

    /**
     * 初始化自动化配置
     * 
     * <p>应用启动时执行，可以添加全局的自动化逻辑。</p>
     */
    @PostConstruct
    public void init() {
        log.info("Initializing subtitle automation configuration");
        
        // 注意：具体的频道回调在 configureAutomationForChannel 中配置
        // 这里可以添加全局的初始化逻辑
        
        log.info("Subtitle automation initialized successfully");
    }

    /**
     * 为指定频道配置自动化处理
     * 
     * <p>注册直播状态变化的回调函数，实现：</p>
     * <ul>
     *     <li>直播开始时：自动设置直播开始时间</li>
     *     <li>直播结束时：自动停止字幕录制</li>
     * </ul>
     * 
     * @param channelId 频道 ID
     */
    public void configureAutomationForChannel(String channelId) {
        log.info("Configuring subtitle automation for channel: {}", channelId);

        // 注册直播开始回调
        channelStatusMonitor.onStreaming(channelId, (id) -> {
            log.info("Stream started detected for channel: {}, auto-setting stream start time", id);
            
            try {
                // 自动设置直播开始时间（用于计算字幕相对时间）
                subtitleService.setStreamStartTime(id);
                log.info("Stream start time set successfully for channel: {}", id);
                
            } catch (Exception e) {
                log.error("Error setting stream start time for channel: {}", id, e);
            }
        });

        // 注册直播结束回调
        channelStatusMonitor.onEndStream(channelId, (id) -> {
            log.info("Stream ended detected for channel: {}, auto-stopping subtitle recording", id);
            
            try {
                // 自动停止字幕录制
                subtitleService.stopRecording(id);
                log.info("Subtitle recording stopped successfully for channel: {}", id);
                
            } catch (Exception e) {
                log.error("Error stopping subtitle recording for channel: {}", id, e);
            }
        });

        log.info("Subtitle automation configured successfully for channel: {}", channelId);
    }
}
