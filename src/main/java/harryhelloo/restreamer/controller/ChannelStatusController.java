package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.config.SubtitleAutomationConfig;
import harryhelloo.restreamer.monitor.ChannelStatusMonitor;
import harryhelloo.restreamer.pojo.StreamPlatform;
import harryhelloo.restreamer.pojo.StreamerChannel;
import harryhelloo.restreamer.service.ChannelService;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 频道状态监控控制器
 *
 * <p>提供频道状态查询、更新和实时监控功能。</p>
 *
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>设置/更新频道信息</li>
 *     <li>获取频道信息（按 ID）</li>
 *     <li>获取频道名称（支持 YouTube）</li>
 *     <li>启动/停止频道状态监控</li>
 *     <li>获取当前频道状态</li>
 *     <li>SSE 实时状态推送</li>
 * </ul>
 *
 * <h2>API 端点：</h2>
 * <ul>
 *     <li>{@code POST /api/channel/set} - 设置频道信息</li>
 *     <li>{@code GET /api/channel/get/byid/{channelId}} - 按 ID 获取频道</li>
 *     <li>{@code GET /api/channel/get/name} - 获取频道名称</li>
 *     <li>{@code POST /api/channel/monitor/start} - 启动监控</li>
 *     <li>{@code POST /api/channel/monitor/stop} - 停止监控</li>
 *     <li>{@code GET /api/channel/monitor/status} - 获取状态</li>
 *     <li>{@code GET /api/channel/monitor/sse} - SSE 实时推送</li>
 * </ul>
 *
 * <h2>平台支持：</h2>
 * <ul>
 *     <li>YouTube：完整支持（通过 yt-dlp）</li>
 *     <li>Twitch：基础支持</li>
 *     <li>Bilibili：暂未实现</li>
 * </ul>
 *
 * @author harryhelloo
 * @version 1.0
 * @see ChannelService
 * @see ChannelStatusMonitor
 */
@Log4j2
@RestController
@RequestMapping("/api/channel")
public class ChannelStatusController {
    @Autowired
    private ChannelService channelService;
    @Autowired
    private ChannelStatusMonitor channelStatusMonitor;
    @Autowired
    private SubtitleAutomationConfig subtitleAutomationConfig;

    /**
     * 设置/更新频道信息
     *
     * @param channel 频道信息对象
     * @return 更新后的频道信息
     */
    @PostMapping("/set")
    public ResponseEntity<StreamerChannel> setChannel(@RequestBody @NonNull StreamerChannel channel) {
        channelService.updateChannel(channel);
        return ResponseEntity.ok(channel);
    }

    /**
     * 根据频道 ID 获取频道信息
     *
     * @param channelId 频道 ID
     * @return 频道信息对象
     */
    @GetMapping("/get/byid/{channelId}")
    public ResponseEntity<StreamerChannel> getChannelById(@PathVariable String channelId) {
        return ResponseEntity.ok(channelService.getChannel(channelId));
    }

    /**
     * 获取频道名称
     *
     * <p>根据平台类型获取频道名称：</p>
     * <ul>
     *     <li>YouTube：通过 yt-dlp 查询</li>
     *     <li>Twitch：直接返回</li>
     *     <li>Bilibili：返回不支持提示</li>
     * </ul>
     *
     * @param channel 频道信息对象
     * @return 包含频道名称的频道信息
     */
    @GetMapping("/get/name")
    public ResponseEntity<StreamerChannel> getChannelName(@RequestBody @NonNull StreamerChannel channel) {
        var channelId = channel.getChannelId();
        var platform = channel.getPlatform();
        switch (platform) {
            case StreamPlatform.YOUTUBE -> {
                try {
                    channel = channelService.getYoutubeChannelName(channel);
                    String channelName = channel.getChannelName();
                    log.info("Get YouTube channel name: {}", channelName);
                    return ResponseEntity.ok(channel);
                } catch (Exception e) {
                    log.error("Failed to get YouTube channel name by id: {}", channelId, e);
                    return ResponseEntity.internalServerError().build();
                }
            }
            case StreamPlatform.TWITCH -> {
                return ResponseEntity.ok(channel);
            }
            case StreamPlatform.BILIBILI -> {
                channel.setChannelName("Bilibili is not supported yet.");
                channelService.updateChannel(channel);
                return ResponseEntity.badRequest().body(channel);
            }
            default -> {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    /**
     * 启动频道状态监控
     *
     * <p>启动监控后，当检测到直播开始时会自动：</p>
     * <ul>
     *     <li>设置直播开始时间（用于字幕时间计算）</li>
     *     <li>触发 onStreaming 回调（如果已配置）</li>
     * </ul>
     * <p>当检测到直播结束时会自动：</p>
     * <ul>
     *     <li>触发 onEndStream 回调（如果已配置）</li>
     *     <li>停止字幕录制</li>
     * </ul>
     *
     * @param channel 频道信息对象
     * @return 操作结果信息
     */
    @PostMapping("/monitor/start")
    public ResponseEntity<String> startMonitoring(@RequestBody @NonNull StreamerChannel channel) {
        try {
            channelStatusMonitor.startMonitoring(channel);

            // 配置自动化处理
            subtitleAutomationConfig.configureAutomationForChannel(channel.getChannelId());

            return ResponseEntity.ok("Monitoring started");
        } catch (Exception e) {
            log.error("Failed to start monitoring", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 停止频道状态监控
     *
     * @param channel 频道信息对象
     * @return 操作结果信息
     */
    @PostMapping("/monitor/stop")
    public ResponseEntity<String> stopMonitoring(@RequestBody @NonNull StreamerChannel channel) {
        try {
            channelStatusMonitor.stopMonitoring(channel);
            return ResponseEntity.ok("Monitoring stopped");
        } catch (Exception e) {
            log.error("Failed to stop monitoring", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 实时推送频道状态（SSE）
     *
     * <p>通过 Server-Sent Events 实时推送频道状态更新。</p>
     *
     * @param channelId 频道 ID
     * @param platform  平台类型
     * @return SSE Emitter，用于推送状态更新
     */
    @GetMapping("/monitor/sse")
    public SseEmitter streamStatus(
        @RequestParam("channelId") String channelId,
        @RequestParam("platform") String platform
    ) {
        StreamerChannel channel = channelService.getChannel(channelId);
        if (channel == null) {
            channel = channelService.updateChannel(StreamerChannel.builder().channelId(channelId).platform(platform).build());
        }
        return channelStatusMonitor.registerSseEmitter(channel);
    }
}