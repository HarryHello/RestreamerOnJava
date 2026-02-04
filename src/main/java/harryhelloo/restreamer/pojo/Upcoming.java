package harryhelloo.restreamer.pojo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Upcoming {
    private String streamId;
    private String streamTitle;
    private int timestamp;
}
