package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.service.SettingsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 设置管理控制器
 *
 * <p>提供系统配置的读取、更新和保存功能。</p>
 *
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>获取当前系统配置</li>
 *     <li>更新配置（内存）</li>
 *     <li>保存配置到文件</li>
 * </ul>
 *
 * <h2>API 端点：</h2>
 * <ul>
 *     <li>{@code GET /api/settings} - 获取所有设置</li>
 *     <li>{@code PUT /api/settings/set} - 更新设置（内存）</li>
 *     <li>{@code POST /api/settings/save} - 保存设置到文件</li>
 * </ul>
 *
 * @author harryhelloo
 * @version 1.0
 * @see Settings
 * @see SettingsService
 */
@Log4j2
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingsService settingsService;

    /**
     * 获取所有设置
     *
     * @return 当前系统配置对象
     */
    @GetMapping
    public ResponseEntity<Settings> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    /**
     * 重新从文件加载所有设置
     *
     * @return 加载得到的配置对象
     */
    @GetMapping("/load")
    public ResponseEntity<Settings> loadSettings() {
        return ResponseEntity.ok(settingsService.loadSettings());
    }

    /**
     * 更新设置（仅内存，不保存到文件）
     *
     * @param settings 新的配置对象
     * @return 更新后的配置对象
     */
    @PutMapping("/set")
    public ResponseEntity<Settings> updateSettings(@RequestBody Settings settings) {
        if (settings == null) {
            return ResponseEntity.badRequest().build();
        }

        settingsService.updateSettings(settings);
        log.info("Settings updated");
        // 返回更新后的设置
        return ResponseEntity.ok(settingsService.getSettings());
    }

    /**
     * 保存当前设置到文件
     *
     * @param settings 要保存的配置对象
     * @return 保存后的配置对象
     */
    @PostMapping("/save")
    public ResponseEntity<Settings> saveSettings(@RequestBody Settings settings) {
        if (settings == null) {
            return ResponseEntity.badRequest().build();
        }
        settingsService.updateSettings(settings);
        settingsService.saveSettings();
        return ResponseEntity.ok(settingsService.getSettings());
    }
}