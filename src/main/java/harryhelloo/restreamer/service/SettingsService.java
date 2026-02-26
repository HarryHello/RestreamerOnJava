package harryhelloo.restreamer.service;

import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.repository.SettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;
    // 获取设置实例
    @Getter
    private Settings settings;

    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @PostConstruct
    public void init() {
        loadSettings();
    }

    // 从文件加载设置
    private void loadSettings() {
        settings = settingsRepository.load();
    }

    // 保存设置到文件
    public void saveSettings() {
        settingsRepository.save(settings);
    }

    // 更新设置并保存
    public void updateSettings(Settings updatedSettings) {
        this.settings = updatedSettings;
        saveSettings();
    }

    // 更新单个设置属性（支持任意深度的深层键，如 "a.b.c.d"）
    public void updateSetting(String key, Object value) {
        try {
            updateSettingRecursive(settings, key, value);
            saveSettings();
        } catch (Exception e) {
            log.error("Failed to update setting: {}", e.getMessage());
            throw new RuntimeException("Failed to update setting: %s".formatted(key), e);
        }
    }

    // 递归更新嵌套属性
    private void updateSettingRecursive(Object target, String key, Object value) throws Exception {
        if (key.contains(".")) {
            // 处理深层键
            String[] parts = key.split("\\.", 2);
            String currentKey = parts[0];
            String remainingKey = parts[1];

            // 获取当前字段
            java.lang.reflect.Field field = target.getClass().getDeclaredField(currentKey);
            field.setAccessible(true);
            Object currentObject = field.get(target);

            if (currentObject == null) {
                // 如果当前对象为null，需要先创建实例
                Class<?> fieldClass = field.getType();
                currentObject = fieldClass.getDeclaredConstructor().newInstance();
                field.set(target, currentObject);
            }

            // 递归处理剩余的键
            updateSettingRecursive(currentObject, remainingKey, value);
        } else {
            // 处理最后一级属性
            java.lang.reflect.Field field = target.getClass().getDeclaredField(key);
            field.setAccessible(true);
            field.set(target, value);
        }
    }
}