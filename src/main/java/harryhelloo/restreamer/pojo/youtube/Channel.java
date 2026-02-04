package harryhelloo.restreamer.pojo.youtube;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class Channel {
    private String channelId;
    private String channelName;

    private boolean isCheckStream;
    private boolean isNoStream;
    private boolean isStreaming;
    private boolean isUpcomingStream;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant scheduledStreamTime; // nullable
    private String streamUrl;
    private String streamTitle;
}
