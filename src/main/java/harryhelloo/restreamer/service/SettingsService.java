package harryhelloo.restreamer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import harryhelloo.restreamer.exception.FileException;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Service
public class SettingsService {

    private static final String SETTINGS_FILE = "settings.json";
    private final Map<String, Object> settings = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    // 从文件加载
    private void loadFromFile() {
        File file = new File(SETTINGS_FILE);

        if (file.exists()) {
            try {
                // 检查文件是否为空
                if (file.length() > 0) {
                    Map<String, Object> loaded = objectMapper.readValue(file, Map.class);
                    settings.putAll(loaded);
                    log.info("设置已从文件加载");
                } else {
                    log.warn("{} 文件为空, 使用默认空设置", SETTINGS_FILE);
                }
            } catch (IOException e) {
                log.error("加载设置失败: {}", e.getMessage());
                throw new FileException("Filed to load from %s".formatted(SETTINGS_FILE));
            }
        } else {
            log.warn("{} 不存在, 创建空设置", SETTINGS_FILE);
            try {
                if (!file.createNewFile()) {
                    throw new FileException("Failed to create %s".formatted(SETTINGS_FILE));
                }
            } catch (IOException e) {
                throw new FileException("Failed to create %s".formatted(SETTINGS_FILE), e);
            }
        }
    }

    // 保存到文件
    private void saveToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(SETTINGS_FILE), settings);
            log.info("设置已保存到文件");
        } catch (IOException e) {
            log.error("保存设置失败: {}", e.getMessage());
            throw new FileException("Failed to save to %s".formatted(SETTINGS_FILE), e);
        }
    }

    // 获取所有设置
    public Map<String, Object> getAllSettings() {
        return new HashMap<>(settings);
    }

    // 更新单个设置
    public void updateSetting(String key, Object value) {
        settings.put(key, value);
        saveToFile(); // 立即保存
    }

    // 获取单个设置
    public Object getSetting(String key) {
        return settings.get(key);
    }

    // 获取设置（带默认值）
    public Object getSetting(String key, Object defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }
}