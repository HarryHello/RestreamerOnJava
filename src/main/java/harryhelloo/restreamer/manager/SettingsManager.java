package harryhelloo.restreamer.manager;

import harryhelloo.restreamer.pojo.Settings;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置配置管理器（单例模式）
 * 
 * <p>负责管理全局的 {@link Settings} 实例，并提供配置变更监听机制。</p>
 * 
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>单例模式管理全局配置</li>
 *     <li>配置变更通知（观察者模式）</li>
 *     <li>线程安全的配置访问</li>
 * </ul>
 * 
 * <h2>使用示例：</h2>
 * <pre>
 * SettingsManager manager = SettingsManager.getInstance();
 * Settings settings = manager.getSettings();
 * settings.setDoTranslate(true);
 * manager.setSettings(settings); // 触发配置变更通知
 * </pre>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see Settings
 * @see ConfigurationChangeListener
 */
@Log4j2
@Component
public class SettingsManager {

    private static volatile SettingsManager instance;
    
    /**
     * 配置变更监听器列表
     */
    private final List<ConfigurationChangeListener> listeners = new ArrayList<>();
    
    /**
     * 当前配置对象
     */
    @Getter
    private Settings settings;

    /**
     * 私有构造函数（单例模式）
     */
    private SettingsManager() {
        this.settings = new Settings();
    }

    /**
     * 获取单例实例（双重检查锁定）
     * 
     * @return SettingsManager 单例对象
     */
    public static SettingsManager getInstance() {
        if (instance == null) {
            synchronized (SettingsManager.class) {
                if (instance == null) {
                    instance = new SettingsManager();
                }
            }
        }
        return instance;
    }

    /**
     * 设置新的配置对象
     *
     * @param settings 新的设置对象
     */
    public void setSettings(@NonNull Settings settings) {
        Settings oldValue = this.settings;
        this.settings = settings;
        notifyConfigurationChanged("settings", oldValue, settings);
    }

    /**
     * 添加配置变更监听器
     *
     * @param listener 监听器
     */
    public void addConfigurationChangeListener(ConfigurationChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除配置变更监听器
     *
     * @param listener 监听器
     */
    public void removeConfigurationChangeListener(ConfigurationChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器配置变更
     *
     * @param key      变更的键
     * @param oldValue 旧值
     * @param newValue 新值
     */
    private void notifyConfigurationChanged(String key, Object oldValue, Object newValue) {
        for (ConfigurationChangeListener listener : listeners) {
            listener.onConfigurationChanged(key, oldValue, newValue, settings);
        }
    }

    /**
     * 配置变更监听器接口
     * 
     * <p>当配置发生变更时，会调用此接口的 {@link #onConfigurationChanged} 方法。</p>
     */
    public interface ConfigurationChangeListener {
        /**
         * 配置变更回调
         * 
         * @param key 变更的键
         * @param oldValue 旧值
         * @param newValue 新值
         * @param settings 完整的配置对象
         */
        void onConfigurationChanged(String key, Object oldValue, Object newValue, Settings settings);
    }
}
