package harryhelloo.restreamer.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 主播频道信息实体
 * 
 * <p>表示一个直播频道的完整信息，包括平台、频道 ID、直播状态、流地址等。</p>
 * 
 * <h2>主要用途：</h2>
 * <ul>
 *     <li>在 ChannelRepository 中持久化存储</li>
 *     <li>在 ChannelService 中更新频道状态</li>
 *     <li>在 SubtitleService 中用于字幕录制</li>
 * </ul>
 * 
 * <h2>状态字段说明：</h2>
 * <ul>
 *     <li>{@code isCheckingStream} - 正在检查直播状态</li>
 *     <li>{@code isNoStream} - 无直播</li>
 *     <li>{@code isStreaming} - 正在直播</li>
 *     <li>{@code isUpcomingStream} - 有即将开始的直播</li>
 * </ul>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see StreamPlatform
 */
@Data
@Builder
public class StreamerChannel {
    
    /**
     * 直播平台类型
     * <p>默认为 YOUTUBE，支持 TWITCH、BILIBILI 等</p>
     */
    @Builder.Default
    private String platform = StreamPlatform.YOUTUBE;

    /**
     * 频道唯一标识符
     * <p>由平台分配的频道 ID</p>
     */
    private String channelId;
    
    /**
     * 频道显示名称
     * <p>用户在平台上显示的频道名</p>
     */
    private String channelName;

    /**
     * 是否正在检查直播状态
     * <p>防止并发检查的标记位</p>
     */
    @Builder.Default
    private boolean isCheckingStream = false;
    
    /**
     * 是否无直播
     * <p>当前没有正在进行的直播</p>
     */
    @Builder.Default
    private boolean isNoStream = false;
    
    /**
     * 是否正在直播
     * <p>当前有正在进行的直播</p>
     */
    @Builder.Default
    private boolean isStreaming = false;
    
    /**
     * 是否有即将开始的直播
     * <p>有预定的直播即将开始</p>
     */
    @Builder.Default
    private boolean isUpcomingStream = false;
    
    /**
     * 预定的直播开始时间
     * <p>UTC 时间格式，仅当 isUpcomingStream=true 时有效</p>
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant scheduledStreamTime; // nullable
    
    /**
     * 直播流地址
     * <p>用于 OBS 拉流或字幕录制</p>
     */
    private String streamUrl;
    
    /**
     * 直播标题
     * <p>当前直播的标题</p>
     */
    private String streamTitle;
}
