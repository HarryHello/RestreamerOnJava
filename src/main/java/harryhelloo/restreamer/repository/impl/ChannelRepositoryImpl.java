package harryhelloo.restreamer.repository.impl;

import harryhelloo.restreamer.pojo.StreamerChannel;
import harryhelloo.restreamer.repository.ChannelRepository;
import lombok.NonNull;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class ChannelRepositoryImpl implements ChannelRepository {
    private final Map<String, StreamerChannel> channels = new HashMap<>();

    @Override
    public StreamerChannel getChannel(@NonNull String channelId) {
        if (channels.containsKey(channelId)) {
            return channels.get(channelId);
        }

        return null;
    }

    @Override
    public StreamerChannel updateChannel(@NonNull StreamerChannel channel) {
        var channelId = channel.getChannelId();
        if (channelId == null || channelId.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel Id is null or empty!");
        }
        channels.put(channel.getChannelId(), channel);
        return channels.get(channel.getChannelId());
    }
}
