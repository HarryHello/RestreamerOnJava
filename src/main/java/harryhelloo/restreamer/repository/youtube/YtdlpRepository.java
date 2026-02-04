package harryhelloo.restreamer.repository.youtube;

import harryhelloo.restreamer.pojo.youtube.Channel;

public interface YtdlpRepository {
    Channel getChannelName(Channel channel, String cookiesPath, String ytdlpPath);

    Channel getChannelStatus(Channel channel, String cookiesPath, String ytdlpPath);
}
