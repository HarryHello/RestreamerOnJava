package harryhelloo.restreamer.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import harryhelloo.restreamer.exception.FileException;
import harryhelloo.restreamer.pojo.Settings;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;

@Log4j2
@Repository
public class SettingsRepository {

    private static final String SETTINGS_FILE = "settings.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void save(Settings settings) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(SETTINGS_FILE), settings);
            log.info("设置已保存到文件");
        } catch (IOException e) {
            log.error("保存设置失败: {}", e.getMessage());
            throw new FileException("Failed to save to %s".formatted(SETTINGS_FILE), e);
        }
    }

    public Settings load() {
        File file = new File(SETTINGS_FILE);

        if (file.exists()) {
            try {
                // 检查文件是否为空
                if (file.length() > 0) {
                    Settings loaded = objectMapper.readValue(file, Settings.class);
                    log.info("设置已从文件加载");
                    return loaded;
                } else {
                    log.warn("{} 文件为空, 使用默认设置", SETTINGS_FILE);
                    return Settings.get();
                }
            } catch (IOException e) {
                log.error("加载设置失败: {}", e.getMessage());
                throw new FileException("Filed to load from %s".formatted(SETTINGS_FILE));
            }
        } else {
            log.warn("{} 不存在, 创建默认设置", SETTINGS_FILE);
            try {
                if (!file.createNewFile()) {
                    throw new FileException("Failed to create %s".formatted(SETTINGS_FILE));
                }
            } catch (IOException e) {
                throw new FileException("Failed to create %s".formatted(SETTINGS_FILE), e);
            }
            return Settings.get();
        }
    }
}