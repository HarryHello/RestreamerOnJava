package harryhelloo.restreamer.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ObsWebsocket {
    private String host;
    private int port;
    private String password;
}
