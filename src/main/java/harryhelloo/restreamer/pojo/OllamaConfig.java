package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaConfig {
    private String host;
    private Integer port;
    private String model;
}
