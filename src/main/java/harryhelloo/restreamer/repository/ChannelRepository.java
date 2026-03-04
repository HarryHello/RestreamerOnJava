package harryhelloo.restreamer.repository;

import harryhelloo.restreamer.pojo.StreamerChannel;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelRepository {
    StreamerChannel getChannel(String channelId);

    StreamerChannel updateChannel(StreamerChannel channel);
}
