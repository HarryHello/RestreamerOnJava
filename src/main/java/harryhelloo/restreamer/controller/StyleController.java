package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.pojo.HistoryStyle;
import harryhelloo.restreamer.pojo.SubtitleStyle;
import harryhelloo.restreamer.service.StyleService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping("/api/subtitles")
public class StyleController {
    @Autowired
    private StyleService styleService;

    @PutMapping("/save-style/subtitle")
    public ResponseEntity<SubtitleStyle> saveSubtitleStyle(@RequestBody SubtitleStyle subtitleStyle) {
        styleService.setCurrentSubtitleStyle(subtitleStyle);
        styleService.saveSubtitleStyleToFile();
        return ResponseEntity.ok(styleService.getCurrentSubtitleStyle());
    }

    @PutMapping("/save-style/history")
    public ResponseEntity<HistoryStyle> saveHistoryStyle(@RequestBody HistoryStyle historyStyle) {
        styleService.setCurrentHistoryStyle(historyStyle);
        styleService.saveHistoryStyleToFile();
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
