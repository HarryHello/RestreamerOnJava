package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.service.SettingsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingsService settingsService;

    // 获取所有设置
    @GetMapping
    public Map<String, Object> getSettings() {
        return settingsService.getAllSettings();
    }

    // 更新单个设置
    @PostMapping("/{key}")
    public ResponseEntity<Map<String, Object>> updateSetting(
        @PathVariable String key,
        @RequestBody Map<String, Object> request) {

        Object value = request.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }

        settingsService.updateSetting(key, value);
        log.info("Setting updated: [{}, {}]", key, value);
        // 返回更新后的完整设置
        return ResponseEntity.ok(settingsService.getAllSettings());
    }
}
