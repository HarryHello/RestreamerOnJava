package harryhelloo.restreamer.monitor;

import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.pojo.Settings;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OBS 媒体监测器初始化服务
 * 
 * 在应用启动时根据 Settings 配置自动启动/停止媒体监测
 */
@Log4j2
@Component
public class ObsMediaMonitorInitializer {

    @Autowired
    private ObsMediaMonitor mediaMonitor;

    @Autowired
    private SettingsManager settingsManager;

    /**
     * 应用启动时初始化媒体监测
     */
    @PostConstruct
    public void init() {
        Settings settings = settingsManager.getSettings();
        
        if (settings == null) {
            log.warn("Settings not available, skipping media monitor initialization");
            return;
        }

        Boolean autoRestart = settings.getDoAutoRestartObsSource();
        String sourceName = settings.getObsSource();

        if (autoRestart == null || !autoRestart) {
            log.info("Auto-restart is disabled in settings, skipping media monitor initialization");
            return;
        }

        if (sourceName == null || sourceName.trim().isEmpty()) {
            log.warn("OBS source name is not configured, skipping media monitor initialization");
            return;
        }

        // 启动媒体监测，默认每 10 秒检查一次
        long checkInterval = 10;  // 秒
        mediaMonitor.startMonitoring(sourceName.trim(), checkInterval);
        
        log.info("Media monitor initialized for source: '{}' (interval: {}s)", sourceName, checkInterval);
    }

    /**
     * 应用关闭时清理资源
     */
    @PreDestroy
    public void destroy() {
        Settings settings = settingsManager.getSettings();
        if (settings != null && settings.getObsSource() != null) {
            mediaMonitor.stopMonitoring(settings.getObsSource());
            log.info("Media monitor stopped");
        }
    }
}
