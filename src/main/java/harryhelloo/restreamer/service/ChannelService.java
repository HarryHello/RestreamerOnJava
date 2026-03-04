package harryhelloo.restreamer.service;

import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.pojo.StreamPlatform;
import harryhelloo.restreamer.pojo.StreamerChannel;
import harryhelloo.restreamer.repository.ChannelRepository;
import harryhelloo.restreamer.repository.youtube.YtdlpRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 频道服务
 * 
 * <p>提供频道信息管理、状态查询和更新功能。</p>
 * 
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>获取频道基本信息</li>
 *     <li>更新频道状态（直播中、无直播、即将开始等）</li>
 *     <li>获取 YouTube 频道名称</li>
 *     <li>根据平台类型查询频道状态</li>
 * </ul>
 * 
 * <h2>平台支持：</h2>
 * <ul>
 *     <li>YouTube：通过 yt-dlp 或 YouTube API 查询</li>
 *     <li>Twitch：基础支持</li>
 *     <li>Bilibili：暂未实现</li>
 * </ul>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see StreamerChannel
 * @see ChannelRepository
 */
@Log4j2
@Service
public class ChannelService {
    
    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private YtdlpRepository ytdlpRepository;

    /**
     * 获取 YouTube 频道名称
     * 
     * <p>通过 yt-dlp 查询 YouTube 频道名称并更新。</p>
     * 
     * @param channel 频道信息对象
     * @return 更新后的频道信息
     */
    public StreamerChannel getYoutubeChannelName(StreamerChannel channel) {
        updateChannel(channel);
        String cookiesPath = SettingsManager.getInstance().getSettings().getYoutubeCookiesPath();
        return updateChannel(ytdlpRepository.getChannelStatus(channel, cookiesPath, null));
    }

    /**
     * 获取频道直播状态
     * 
     * <p>根据平台类型查询频道当前的直播状态。</p>
     * 
     * @param channel 频道信息对象
     * @return 更新后的频道状态信息
     */
    public StreamerChannel getChannelStatus(StreamerChannel channel) {
        updateChannel(channel);
        if (channel.getPlatform().equals(StreamPlatform.YOUTUBE)) {
            String cookiesPath = SettingsManager.getInstance().getSettings().getYoutubeCookiesPath();
            return updateChannel(ytdlpRepository.getChannelStatus(channel, cookiesPath, null));
        } else {
            return setUnknownStatus(channel);
        }
    }

    /**
     * 根据频道 ID 获取频道信息
     * 
     * @param channelId 频道 ID
     * @return 频道信息对象
     */
    public StreamerChannel getChannel(String channelId) {
        return channelRepository.getChannel(channelId);
    }

    /**
     * 更新频道信息
     * 
     * @param channel 频道信息对象
     * @return 更新后的频道信息
     */
    public StreamerChannel updateChannel(StreamerChannel channel) {
        return channelRepository.updateChannel(channel);
    }

    /**
     * 设置未知状态
     * 
     * <p>将所有状态标志设置为 false，用于不支持的平台。</p>
     * 
     * @param channel 频道信息对象
     * @return 更新后的频道信息
     */
    public StreamerChannel setUnknownStatus(StreamerChannel channel) {
        channel.setCheckingStream(false);
        channel.setStreaming(false);
        channel.setNoStream(false);
        channel.setUpcomingStream(false);
        return updateChannel(channel);
    }
}