package harryhelloo.restreamer.service;

import harryhelloo.restreamer.pojo.youtube.Channel;
import harryhelloo.restreamer.repository.youtube.YtdlpRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class ChannelService {
    @Autowired
    YtdlpRepository ytdlpRepository;

    @Autowired
    SettingsService settingsService;

    public Channel getChannelName(String channelId) {
        Channel channel = Channel.builder()
            .channelId(channelId)
            .build();
        String cookiesPath = (String) settingsService.getSetting("ytdlp-cookies-path");
        return ytdlpRepository.getChannelName(channel, cookiesPath, null);
    }

    public Channel getChannelStatus(String channelId) {
        Channel channel = Channel.builder()
            .channelId(channelId)
            .build();
        String cookiesPath = (String) settingsService.getSetting("ytdlp-cookies-path");
        return ytdlpRepository.getChannelStatus(channel, cookiesPath, null);
    }
}
