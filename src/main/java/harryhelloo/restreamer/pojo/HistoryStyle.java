package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryStyle {
    private String mainBgColor = "#000000";
    private Double mainBgOpacity = 0.60;
    private String cardBgColor = "#000000";
    private Double cardBgOpacity = 0.40;
    private String englishColor = "#ffffff";
    private String englishStrokeColor = "#0033cc";
    private Number englishSize = 36;
    private String chineseColor = "#ffffff";
    private String chineseStrokeColor = "#0033cc";
    private Number chineseSize = 36;

    public static HistoryStyle getDefault() {
        return HistoryStyle.builder()
            .mainBgColor("#000000")
            .mainBgOpacity(0.60)
            .cardBgColor("#000000")
            .cardBgOpacity(0.40)
            .englishColor("#ffffff")
            .englishStrokeColor("#0033cc")
            .englishSize(36)
            .chineseColor("#ffffff")
            .chineseStrokeColor("#0033cc")
            .chineseSize(36)
            .build();
    }
}
