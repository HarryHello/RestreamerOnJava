package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 历史记录样式配置
 * 
 * <p>定义 OBS 历史记录区域的显示样式，包括背景、卡片、字体颜色等属性。</p>
 * 
 * <h2>样式属性：</h2>
 * <ul>
 *     <li><strong>主背景：</strong>颜色、透明度</li>
 *     <li><strong>卡片背景：</strong>颜色、透明度</li>
 *     <li><strong>英文字幕：</strong>颜色、描边颜色、字体大小</li>
 *     <li><strong>中文字幕：</strong>颜色、描边颜色、字体大小</li>
 * </ul>
 * 
 * <h2>颜色格式：</h2>
 * <p>使用十六进制颜色码（如 {@code #000000} 表示黑色）</p>
 * 
 * @author harryhelloo
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryStyle {
    
    /**
     * 主背景颜色（十六进制）
     */
    @Builder.Default
    private String mainBgColor = "#000000";
    
    /**
     * 主背景透明度（0.0-1.0）
     */
    @Builder.Default
    private Double mainBgOpacity = 0.60;
    
    /**
     * 卡片背景颜色（十六进制）
     */
    @Builder.Default
    private String cardBgColor = "#000000";
    
    /**
     * 卡片背景透明度（0.0-1.0）
     */
    @Builder.Default
    private Double cardBgOpacity = 0.40;
    
    /**
     * 英文字幕颜色（十六进制）
     */
    @Builder.Default
    private String englishColor = "#ffffff";
    
    /**
     * 英文字幕描边颜色（十六进制）
     */
    @Builder.Default
    private String englishStrokeColor = "#0033cc";
    
    /**
     * 英文字幕字体大小（像素）
     */
    @Builder.Default
    private Number englishSize = 36;
    
    /**
     * 中文字幕颜色（十六进制）
     */
    @Builder.Default
    private String chineseColor = "#ffffff";
    
    /**
     * 中文字幕描边颜色（十六进制）
     */
    @Builder.Default
    private String chineseStrokeColor = "#0033cc";
    
    /**
     * 中文字幕字体大小（像素）
     */
    @Builder.Default
    private Number chineseSize = 36;

    /**
     * 获取默认历史记录样式
     * 
     * @return 默认历史记录样式对象
     */
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
