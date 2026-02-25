package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.service.TranslateService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/subtitles")
public class TranslateController {
    @Autowired
    private TranslateService translateService;

    @PostMapping("/translate")
    public ResponseEntity<String> translate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String sourceLang = request.get("from");
        String targetLang = request.get("to");

        String translated = translateService.translateOneLine(text, sourceLang, targetLang);
        return ResponseEntity.ok(translated);
    }
}
