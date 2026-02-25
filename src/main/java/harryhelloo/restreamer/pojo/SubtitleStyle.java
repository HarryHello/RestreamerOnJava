package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtitleStyle {
    private Boolean showChinese = true;
    private String chineseColor = "#ffffff";
    private String chineseStrokeColor = "#000000";
    private Number chineseSize = 48;

    private Boolean showEnglishFinal = true;
    private String englishFinalColor = "#ffffff";
    private String englishFinalStrokeColor = "#000000";
    private Number englishFinalSize = 36;

    private Boolean showEnglishRealtime = true;
    private String englishRealtimeColor = "#cccccc";
    private String englishRealtimeStrokeColor = "#000000";
    private Number englishRealtimeSize = 36;

    private Double bgOpacity = 0.60;

    public static SubtitleStyle getDefault() {
        return SubtitleStyle.builder()
            .showChinese(true)
            .chineseColor("#ffffff")
            .chineseStrokeColor("#000000")
            .chineseSize(48)
            .showEnglishFinal(true)
            .englishFinalColor("#ffffff")
            .englishFinalStrokeColor("#000000")
            .englishFinalSize(36)
            .showEnglishRealtime(true)
            .englishRealtimeColor("#cccccc")
            .englishRealtimeStrokeColor("#000000")
            .englishRealtimeSize(36)
            .bgOpacity(0.60)
            .build();
    }
}
