package harryhelloo.restreamer.repository.youtube;

import harryhelloo.restreamer.pojo.youtube.Channel;

public interface YtdlpRepository {
    Channel getChannelInfo(Channel channel, String cookiesPath, String ytdlpPath);
}
