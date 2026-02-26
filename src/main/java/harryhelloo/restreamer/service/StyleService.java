package harryhelloo.restreamer.service;

import harryhelloo.restreamer.pojo.HistoryStyle;
import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.pojo.SubtitleStyle;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class StyleService {

    @PostConstruct
    public void init() {
        // 初始化时确保样式配置存在
        ensureStylesExist();
    }

    private void ensureStylesExist() {
        Settings settings = Settings.get();
        
        // 确保字幕样式存在
        if (settings.getSubtitleStyle() == null) {
            log.warn("字幕样式未设置, 使用默认样式");
            settings.setSubtitleStyle(SubtitleStyle.getDefault());
        }
        
        // 确保历史样式存在
        if (settings.getHistoryStyle() == null) {
            log.warn("历史样式未设置, 使用默认样式");
            settings.setHistoryStyle(HistoryStyle.getDefault());
        }
    }

    // 获取字幕样式
    public SubtitleStyle getCurrentSubtitleStyle() {
        SubtitleStyle style = Settings.get().getSubtitleStyle();
        if (style == null) {
            style = SubtitleStyle.getDefault();
            Settings.get().setSubtitleStyle(style);
        }
        return style;
    }

    // 设置字幕样式
    public void setCurrentSubtitleStyle(SubtitleStyle style) {
        Settings.get().setSubtitleStyle(style);
        log.info("字幕样式已更新");
    }

    // 获取历史样式
    public HistoryStyle getCurrentHistoryStyle() {
        HistoryStyle style = Settings.get().getHistoryStyle();
        if (style == null) {
            style = HistoryStyle.getDefault();
            Settings.get().setHistoryStyle(style);
        }
        return style;
    }

    // 设置历史样式
    public void setCurrentHistoryStyle(HistoryStyle style) {
        Settings.get().setHistoryStyle(style);
        log.info("历史样式已更新");
    }
}