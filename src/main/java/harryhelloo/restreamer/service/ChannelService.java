package harryhelloo.restreamer.service;

import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.pojo.youtube.Channel;
import harryhelloo.restreamer.repository.youtube.YtdlpRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class ChannelService {
    @Autowired
    private YtdlpRepository ytdlpRepository;

    private Channel channel;

    public Channel getChannelName(String channelId) {
        channel = Channel.builder()
            .channelId(channelId)
            .build();
        String cookiesPath = Settings.get().getYoutubeCookiesPath();
        return ytdlpRepository.getChannelName(channel, cookiesPath, null);
    }

    public Channel getChannelStatus(String channelId) {
        channel = Channel.builder()
            .channelId(channelId)
            .build();
        String cookiesPath = Settings.get().getYoutubeCookiesPath();
        return ytdlpRepository.getChannelStatus(channel, cookiesPath, null);
    }
}