package harryhelloo.restreamer.service;

import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.repository.SettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 设置服务
 *
 * <p>提供系统配置的加载、保存和更新功能。</p>
 *
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>应用启动时自动加载配置</li>
 *     <li>保存配置到文件</li>
 *     <li>更新内存中的配置</li>
 * </ul>
 *
 * <h2>配置持久化：</h2>
 * <p>配置通过 {@link SettingsRepository} 持久化到本地文件，
 * 支持 JSON 格式存储。</p>
 *
 * @author harryhelloo
 * @version 1.0
 * @see Settings
 * @see SettingsRepository
 */
@Log4j2
@Service
public class SettingsService {

    @Autowired
    private SettingsRepository settingsRepository;

    /**
     * 当前系统配置对象
     */
    @Getter
    private Settings settings;

    /**
     * 初始化方法，应用启动时自动加载配置
     */
    @PostConstruct
    public void init() {
        loadSettings();
    }

    /**
     * 从文件加载设置
     */
    public Settings loadSettings() {
        return settings = settingsRepository.load();
    }

    /**
     * 保存设置到文件
     */
    public void saveSettings() {
        settingsRepository.save(settings);
    }

    /**
     * 更新设置（仅内存）
     *
     * @param updatedSettings 更新后的配置对象
     */
    public void updateSettings(Settings updatedSettings) {
        this.settings = updatedSettings;
    }
}