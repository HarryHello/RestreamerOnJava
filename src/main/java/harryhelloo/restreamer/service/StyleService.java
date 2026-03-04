package harryhelloo.restreamer.service;

import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.pojo.HistoryStyle;
import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.pojo.SubtitleStyle;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * 样式服务
 * 
 * <p>管理字幕样式和历史记录样式的配置。</p>
 * 
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>初始化时确保样式配置存在</li>
 *     <li>获取/设置字幕样式</li>
 *     <li>获取/设置历史记录样式</li>
 *     <li>提供默认样式</li>
 * </ul>
 * 
 * <h2>样式类型：</h2>
 * <ul>
 *     <li>{@link SubtitleStyle} - 字幕显示样式（字体、颜色、大小等）</li>
 *     <li>{@link HistoryStyle} - 历史记录显示样式</li>
 * </ul>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see SubtitleStyle
 * @see HistoryStyle
 */
@Log4j2
@Service
public class StyleService {

    /**
     * 初始化样式配置
     * <p>确保字幕样式和历史样式配置存在，如不存在则使用默认值</p>
     */
    @PostConstruct
    public void init() {
        // 初始化时确保样式配置存在
        ensureStylesExist();
    }

    /**
     * 确保样式配置存在
     * <p>如果样式未设置，则使用默认样式</p>
     */
    private void ensureStylesExist() {
        Settings settings = SettingsManager.getInstance().getSettings();

        // 确保字幕样式存在
        if (settings.getSubtitleStyle() == null) {
            log.warn("字幕样式未设置，使用默认样式");
            settings.setSubtitleStyle(SubtitleStyle.getDefault());
        }

        // 确保历史样式存在
        if (settings.getHistoryStyle() == null) {
            log.warn("历史样式未设置，使用默认样式");
            settings.setHistoryStyle(HistoryStyle.getDefault());
        }
    }

    /**
     * 获取当前字幕样式
     * 
     * @return 当前字幕样式，如未设置则返回默认样式
     */
    public SubtitleStyle getCurrentSubtitleStyle() {
        SubtitleStyle style = SettingsManager.getInstance().getSettings().getSubtitleStyle();
        if (style == null) {
            style = SubtitleStyle.getDefault();
            SettingsManager.getInstance().getSettings().setSubtitleStyle(style);
        }
        return style;
    }

    /**
     * 设置当前字幕样式
     * 
     * @param style 字幕样式对象
     */
    public void setCurrentSubtitleStyle(SubtitleStyle style) {
        SettingsManager.getInstance().getSettings().setSubtitleStyle(style);
        log.info("字幕样式已更新");
    }

    /**
     * 获取当前历史样式
     * 
     * @return 当前历史样式，如未设置则返回默认样式
     */
    public HistoryStyle getCurrentHistoryStyle() {
        HistoryStyle style = SettingsManager.getInstance().getSettings().getHistoryStyle();
        if (style == null) {
            style = HistoryStyle.getDefault();
            SettingsManager.getInstance().getSettings().setHistoryStyle(style);
        }
        return style;
    }

    /**
     * 设置当前历史样式
     * 
     * @param style 历史样式对象
     */
    public void setCurrentHistoryStyle(HistoryStyle style) {
        SettingsManager.getInstance().getSettings().setHistoryStyle(style);
        log.info("历史样式已更新");
    }
}
