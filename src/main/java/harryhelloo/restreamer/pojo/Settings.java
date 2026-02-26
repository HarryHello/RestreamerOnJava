package harryhelloo.restreamer.pojo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class Settings {
    private static volatile Settings instance;
    private final List<ConfigurationChangeListener> listeners = new ArrayList<>();

    private Settings() {
    }

    public static Settings get() {
        if (instance == null) {
            synchronized (Settings.class) {
                if (instance == null) {
                    instance = new Settings();
                }
            }
        }
        return instance;
    }

    // 配置变更监听器接口
    public interface ConfigurationChangeListener {
        void onConfigurationChanged(String key, Object oldValue, Object newValue, Settings settings);
    }

    // 添加配置变更监听器
    public void addConfigurationChangeListener(ConfigurationChangeListener listener) {
        listeners.add(listener);
    }

    // 移除配置变更监听器
    public void removeConfigurationChangeListener(ConfigurationChangeListener listener) {
        listeners.remove(listener);
    }

    // 通知所有监听器配置变更
    private void notifyConfigurationChanged(String key, Object oldValue, Object newValue) {
        for (ConfigurationChangeListener listener : listeners) {
            listener.onConfigurationChanged(key, oldValue, newValue, this);
        }
    }

    private String ChannelId;

    private String StatusCheckTool = "ytdlp"; // ytdlp / youtubeapi

    private String YoutubeCookiesPath; // for ytdlp
    private String YoutubeApiKey; // for youtubeapi

    private ObsWebsocket obsWebsocket;

    private String translationProducer = "ollama"; // ollama / openai / deepl
    private OllamaConfig ollamaConfig;
    private String deeplApi;
    private OpenaiConfig openaiConfig;
    private String sourceLang;
    private String TargetLang;

    private SubtitleStyle subtitleStyle;
    private HistoryStyle historyStyle;

    // 重写setter方法，添加配置变更通知
    public void setChannelId(String channelId) {
        String oldValue = this.ChannelId;
        this.ChannelId = channelId;
        notifyConfigurationChanged("ChannelId", oldValue, channelId);
    }

    public void setStatusCheckTool(String statusCheckTool) {
        String oldValue = this.StatusCheckTool;
        this.StatusCheckTool = statusCheckTool;
        notifyConfigurationChanged("StatusCheckTool", oldValue, statusCheckTool);
    }

    public void setYoutubeCookiesPath(String youtubeCookiesPath) {
        String oldValue = this.YoutubeCookiesPath;
        this.YoutubeCookiesPath = youtubeCookiesPath;
        notifyConfigurationChanged("YoutubeCookiesPath", oldValue, youtubeCookiesPath);
    }

    public void setYoutubeApiKey(String youtubeApiKey) {
        String oldValue = this.YoutubeApiKey;
        this.YoutubeApiKey = youtubeApiKey;
        notifyConfigurationChanged("YoutubeApiKey", oldValue, youtubeApiKey);
    }

    public void setObsWebsocket(ObsWebsocket obsWebsocket) {
        ObsWebsocket oldValue = this.obsWebsocket;
        this.obsWebsocket = obsWebsocket;
        notifyConfigurationChanged("obsWebsocket", oldValue, obsWebsocket);
    }

    public void setTranslationProducer(String translationProducer) {
        String oldValue = this.translationProducer;
        this.translationProducer = translationProducer;
        notifyConfigurationChanged("translationProducer", oldValue, translationProducer);
    }

    public void setOllamaConfig(OllamaConfig ollamaConfig) {
        OllamaConfig oldValue = this.ollamaConfig;
        this.ollamaConfig = ollamaConfig;
        notifyConfigurationChanged("ollamaConfig", oldValue, ollamaConfig);
    }

    public void setDeeplApi(String deeplApi) {
        String oldValue = this.deeplApi;
        this.deeplApi = deeplApi;
        notifyConfigurationChanged("deeplApi", oldValue, deeplApi);
    }

    public void setOpenaiConfig(OpenaiConfig openaiConfig) {
        OpenaiConfig oldValue = this.openaiConfig;
        this.openaiConfig = openaiConfig;
        notifyConfigurationChanged("openaiConfig", oldValue, openaiConfig);
    }

    public void setSourceLang(String sourceLang) {
        String oldValue = this.sourceLang;
        this.sourceLang = sourceLang;
        notifyConfigurationChanged("sourceLang", oldValue, sourceLang);
    }

    public void setTargetLang(String targetLang) {
        String oldValue = this.TargetLang;
        this.TargetLang = targetLang;
        notifyConfigurationChanged("TargetLang", oldValue, targetLang);
    }

    public void setSubtitleStyle(SubtitleStyle subtitleStyle) {
        SubtitleStyle oldValue = this.subtitleStyle;
        this.subtitleStyle = subtitleStyle;
        notifyConfigurationChanged("subtitleStyle", oldValue, subtitleStyle);
    }

    public void setHistoryStyle(HistoryStyle historyStyle) {
        HistoryStyle oldValue = this.historyStyle;
        this.historyStyle = historyStyle;
        notifyConfigurationChanged("historyStyle", oldValue, historyStyle);
    }
}