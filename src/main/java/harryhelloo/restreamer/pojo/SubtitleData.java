package harryhelloo.restreamer.pojo;

import lombok.Builder;
import lombok.Data;

/**
 * 字幕数据传输对象
 * 
 * <p>用于接收前端发送的字幕识别数据，包含字幕的文本内容和高精度的时间信息。</p>
 * 
 * <h2>使用说明：</h2>
 * <ul>
 *     <li>由 Web Speech API 识别字幕后生成</li>
 *     <li>startTime 和 endTime 使用绝对时间戳（毫秒）</li>
 *     <li>后端会自动计算相对于直播开始时间的偏移量</li>
 * </ul>
 * 
 * <h2>时间格式：</h2>
 * <pre>
 * {
 *   "startTime": 1709251200000,  // System.currentTimeMillis()
 *   "endTime": 1709251203500
 * }
 * </pre>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see harryhelloo.restreamer.controller.SubtitleController#processSubtitle
 */
@Data
@Builder
public class SubtitleData {
    
    /**
     * 频道唯一标识符
     * <p>用于标识字幕所属的直播频道</p>
     */
    private String channelId;
    
    /**
     * 字幕文本内容
     * <p>识别到的语音转文字内容</p>
     */
    private String text;
    
    /**
     * 字幕开始时间（毫秒，绝对时间戳）
     * <p>使用 System.currentTimeMillis() 获取的时间戳</p>
     */
    private Long startTime;
    
    /**
     * 字幕结束时间（毫秒，绝对时间戳）
     * <p>使用 System.currentTimeMillis() 获取的时间戳，应该大于 startTime</p>
     */
    private Long endTime;
}
