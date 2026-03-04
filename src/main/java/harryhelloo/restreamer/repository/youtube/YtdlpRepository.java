package harryhelloo.restreamer.repository.youtube;

import harryhelloo.restreamer.pojo.StreamerChannel;

public interface YtdlpRepository {
    StreamerChannel getChannelName(StreamerChannel channel, String cookiesPath, String ytdlpPath);

    StreamerChannel getChannelStatus(StreamerChannel channel, String cookiesPath, String ytdlpPath);
}
