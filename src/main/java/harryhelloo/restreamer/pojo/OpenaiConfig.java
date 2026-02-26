package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenaiConfig {
    private String baseUrl;
    private String apiKey;
    private String model;
}
