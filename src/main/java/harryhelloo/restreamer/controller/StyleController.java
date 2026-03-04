package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.pojo.HistoryStyle;
import harryhelloo.restreamer.pojo.SubtitleStyle;
import harryhelloo.restreamer.service.StyleService;
import harryhelloo.restreamer.service.SettingsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 字幕样式控制器
 * 
 * <p>提供字幕样式和历史记录样式的保存和加载功能。</p>
 * 
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>保存字幕样式配置</li>
 *     <li>保存历史记录样式配置</li>
 *     <li>加载字幕样式配置</li>
 *     <li>加载历史记录样式配置</li>
 * </ul>
 * 
 * <h2>API 端点：</h2>
 * <ul>
 *     <li>{@code PUT /api/subtitles/save-style/subtitle} - 保存字幕样式</li>
 *     <li>{@code PUT /api/subtitles/save-style/history} - 保存历史记录样式</li>
 *     <li>{@code GET /api/subtitles/get-style/subtitle} - 加载字幕样式</li>
 *     <li>{@code GET /api/subtitles/get-style/history} - 加载历史记录样式</li>
 * </ul>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see StyleService
 * @see SubtitleStyle
 * @see HistoryStyle
 */
@Log4j2
@RestController
@RequestMapping("/api/subtitles")
public class StyleController {
    @Autowired
    private StyleService styleService;
    @Autowired
    private SettingsService settingsService;

    /**
     * 保存字幕样式配置
     * 
     * @param subtitleStyle 字幕样式对象
     * @return 保存后的样式对象
     */
    @PutMapping("/save-style/subtitle")
    public ResponseEntity<SubtitleStyle> saveSubtitleStyle(@RequestBody SubtitleStyle subtitleStyle) {
        styleService.setCurrentSubtitleStyle(subtitleStyle);
        settingsService.saveSettings(); // 保存到Settings文件
        return ResponseEntity.ok(styleService.getCurrentSubtitleStyle());
    }

    @PutMapping("/save-style/history")
    public ResponseEntity<HistoryStyle> saveHistoryStyle(@RequestBody HistoryStyle historyStyle) {
        styleService.setCurrentHistoryStyle(historyStyle);
        settingsService.saveSettings(); // 保存到Settings文件
        return ResponseEntity.ok(styleService.getCurrentHistoryStyle());
    }

    @GetMapping("/get-style/subtitle")
    public ResponseEntity<SubtitleStyle> loadSubtitleStyle() {
        return ResponseEntity.ok(styleService.getCurrentSubtitleStyle());
    }

    @GetMapping("/get-style/history")
    public ResponseEntity<HistoryStyle> loadHistoryStyle() {
        return ResponseEntity.ok(styleService.getCurrentHistoryStyle());
    }
}