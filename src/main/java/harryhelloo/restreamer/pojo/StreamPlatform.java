package harryhelloo.restreamer.pojo;

/**
 * 直播平台常量
 * 
 * <p>定义支持的直播平台标识符。</p>
 * 
 * <h2>支持的平台：</h2>
 * <ul>
 *     <li>{@code YOUTUBE} - YouTube</li>
 *     <li>{@code TWITCH} - Twitch</li>
 *     <li>{@code BILIBILI} - 哔哩哔哩（暂未实现）</li>
 * </ul>
 * 
 * <h2>使用示例：</h2>
 * <pre>
 * if (channel.getPlatform().equals(StreamPlatform.YOUTUBE)) {
 *     // 处理 YouTube 频道
 * }
 * </pre>
 * 
 * @author harryhelloo
 * @version 1.0
 */
public class StreamPlatform {
    
    /**
     * YouTube 平台标识符
     */
    public static final String YOUTUBE = "youtube";
    
    /**
     * Twitch 平台标识符
     */
    public static final String TWITCH = "twitch";
    
    /**
     * 哔哩哔哩平台标识符
     */
    public static final String BILIBILI = "bilibili";

    /**
     * 私有构造函数，防止实例化
     */
    private StreamPlatform() {}
}
