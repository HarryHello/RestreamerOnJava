package harryhelloo.restreamer.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统配置设置
 * 
 * <p>包含应用程序的所有配置选项，通过 SettingsManager 统一管理。</p>
 * 
 * <h2>配置分类：</h2>
 * <ul>
 *     <li><strong>频道配置：</strong>ChannelId、StatusCheckTool、YouTube 相关</li>
 *     <li><strong>OBS 配置：</strong>obsWebsocket、doObsRecord、doAutoRestartObsSource 等</li>
 *     <li><strong>字幕配置：</strong>doRecognize、doTranslate、translationProducer 等</li>
 *     <li><strong>样式配置：</strong>subtitleStyle、historyStyle</li>
 * </ul>
 * 
 * <h2>翻译服务配置：</h2>
 * <ul>
 *     <li>translationProducer: "ollama" / "openai" / "deepl"</li>
 *     <li>根据 producer 不同，配置对应的 ollamaConfig、openaiConfig 或 deeplAuthKey</li>
 * </ul>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see harryhelloo.restreamer.manager.SettingsManager
 */
@Data
@NoArgsConstructor
public class Settings {
    
    /**
     * 当前频道 ID
     */
    private String ChannelId;

    /**
     * 直播状态检查工具
     * <p>可选值："ytdlp"（yt-dlp）或 "youtubeapi"（YouTube Data API）</p>
     */
    private String StatusCheckTool = "ytdlp"; // ytdlp / youtubeapi
    
    /**
     * YouTube Cookie 文件路径
     * <p>用于 yt-dlp 访问会员限定内容，仅当 StatusCheckTool="ytdlp" 时使用</p>
     */
    private String YoutubeCookiesPath; // for ytdlp
    
    /**
     * YouTube Data API Key
     * <p>用于 YouTube API 调用，仅当 StatusCheckTool="youtubeapi" 时使用</p>
     */
    private String YoutubeApiKey; // for youtubeapi

    /**
     * OBS WebSocket 连接配置
     */
    private ObsWebsocket obsWebsocket;
    
    /**
     * 是否启用 OBS 直播录制
     */
    private Boolean doObsRecord = false; // 是否录制直播
    
    /**
     * 是否启用 OBS 媒体源自动重启
     * <p>当媒体源停止或暂停时自动重启</p>
     */
    private Boolean doAutoRestartObsSource = true;  // 是否自动重启 OBS 媒体源
    
    /**
     * OBS 场景名称
     * <p>用于定位包含媒体源的场景</p>
     */
    private String ObsScene = "restreamer";         // OBS 场景名称
    
    /**
     * OBS 媒体源名称
     * <p>用于自动重启的媒体源标识</p>
     */
    private String ObsSource = "stream";            // OBS 媒体源名称

    /**
     * 是否启用语音识别
     * <p>启用后会记录原文字幕</p>
     */
    private Boolean doRecognize = true; // 是否启用识别
    
    /**
     * 是否启用翻译
     * <p>启用后会将识别到的字幕进行翻译并记录</p>
     */
    private Boolean doTranslate = true; // 是否启用翻译

    /**
     * 翻译服务提供者
     * <p>可选值："ollama" / "openai" / "deepl"</p>
     */
    private String translationProducer = "ollama"; // ollama / openai / deepl
    
    /**
     * Ollama 配置
     * <p>当 translationProducer="ollama" 时使用</p>
     */
    private OllamaConfig ollamaConfig;
    
    /**
     * DeepL 认证密钥
     * <p>当 translationProducer="deepl" 时使用</p>
     */
    private String deeplAuthKey;
    
    /**
     * OpenAI 配置
     * <p>当 translationProducer="openai" 时使用</p>
     */
    private OpenaiConfig openaiConfig;
    
    /**
     * 源语言代码
     * <p>如 "en"（英语）、"zh"（中文）等</p>
     */
    private String sourceLang;
    
    /**
     * 目标语言代码
     * <p>翻译后的语言，如 "zh"（中文）、"ja"（日语）等</p>
     */
    private String TargetLang;

    /**
     * 字幕样式配置
     */
    private SubtitleStyle subtitleStyle;
    
    /**
     * 历史记录样式配置
     */
    private HistoryStyle historyStyle;

}
