package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.service.SettingsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingsService settingsService;

    // 获取所有设置
    @GetMapping
    public ResponseEntity<Settings> fetchSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    // 更新设置
    @PutMapping
    public ResponseEntity<Settings> updateSettings(@RequestBody Settings settings) {
        if (settings == null) {
            return ResponseEntity.badRequest().build();
        }

        settingsService.updateSettings(settings);
        log.info("Settings updated");
        // 返回更新后的设置
        return ResponseEntity.ok(settingsService.getSettings());
    }

    // 保存当前设置
    @PostMapping("/save")
    public ResponseEntity<Settings> saveSettings() {
        settingsService.saveSettings();
        return ResponseEntity.ok(settingsService.getSettings());
    }

    // 更新单个设置
    @PostMapping("/{key}")
    public ResponseEntity<Settings> updateSetting(
        @PathVariable String key,
        @RequestBody Object value) {
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }

        settingsService.updateSetting(key, value);
        log.info("Setting updated: [{}, {}]", key, value);
        // 返回更新后的完整设置
        return ResponseEntity.ok(settingsService.getSettings());
    }
}