package harryhelloo.restreamer.pojo.youtube;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class Channel {
    private String channelId;
    private String channelName;

    private boolean isNoStream;
    private boolean isStreaming;
    private boolean isUpcomingStream;
    private String scheduledStreamTime; // nullable
    private boolean isCheckStream;
    private String streamUrl;
}
