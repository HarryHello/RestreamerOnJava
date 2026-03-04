package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字幕样式配置
 * 
 * <p>定义 OBS 字幕的显示样式，包括中英文字幕的颜色、大小、描边等属性。</p>
 * 
 * <h2>样式属性：</h2>
 * <ul>
 *     <li><strong>中文字幕：</strong>显示开关、颜色、描边颜色、字体大小</li>
 *     <li><strong>英文终稿：</strong>显示开关、颜色、描边颜色、字体大小</li>
 *     <li><strong>英文实时：</strong>显示开关、颜色、描边颜色、字体大小</li>
 *     <li><strong>背景透明度：</strong>字幕背景的不透明度（0.0-1.0）</li>
 * </ul>
 * 
 * <h2>颜色格式：</h2>
 * <p>使用十六进制颜色码（如 {@code #ffffff} 表示白色）</p>
 * 
 * @author harryhelloo
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtitleStyle {
    
    /**
     * 是否显示中文字幕
     */
    @Builder.Default
    private Boolean showChinese = true;
    
    /**
     * 中文字幕颜色（十六进制）
     */
    @Builder.Default
    private String chineseColor = "#ffffff";
    
    /**
     * 中文字幕描边颜色（十六进制）
     */
    @Builder.Default
    private String chineseStrokeColor = "#000000";
    
    /**
     * 中文字幕字体大小（像素）
     */
    @Builder.Default
    private Number chineseSize = 48;

    /**
     * 是否显示英文终稿字幕
     */
    @Builder.Default
    private Boolean showEnglishFinal = true;
    
    /**
     * 英文终稿字幕颜色（十六进制）
     */
    @Builder.Default
    private String englishFinalColor = "#ffffff";
    
    /**
     * 英文终稿字幕描边颜色（十六进制）
     */
    @Builder.Default
    private String englishFinalStrokeColor = "#000000";
    
    /**
     * 英文终稿字幕字体大小（像素）
     */
    @Builder.Default
    private Number englishFinalSize = 36;

    /**
     * 是否显示英文实时字幕
     */
    @Builder.Default
    private Boolean showEnglishRealtime = true;
    
    /**
     * 英文实时字幕颜色（十六进制）
     */
    @Builder.Default
    private String englishRealtimeColor = "#cccccc";
    
    /**
     * 英文实时字幕描边颜色（十六进制）
     */
    @Builder.Default
    private String englishRealtimeStrokeColor = "#000000";
    
    /**
     * 英文实时字幕字体大小（像素）
     */
    @Builder.Default
    private Number englishRealtimeSize = 36;

    /**
     * 背景透明度（0.0-1.0）
     */
    @Builder.Default
    private Double bgOpacity = 0.60;

    /**
     * 获取默认字幕样式
     * 
     * @return 默认字幕样式对象
     */
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
