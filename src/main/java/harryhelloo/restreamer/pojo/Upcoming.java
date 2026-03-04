package harryhelloo.restreamer.pojo;

import lombok.Builder;
import lombok.Getter;

/**
 * 即将开始的直播信息
 * 
 * <p>表示即将开始的直播的相关信息。</p>
 * 
 * <h2>数据字段：</h2>
 * <ul>
 *     <li>streamId: 直播 ID</li>
 *     <li>streamTitle: 直播标题</li>
 *     <li>timestamp: 直播开始时间戳（Unix 时间戳）</li>
 * </ul>
 * 
 * @author harryhelloo
 * @version 1.0
 */
@Getter
@Builder
public class Upcoming {
    
    /**
     * 直播 ID
     */
    private String streamId;
    
    /**
     * 直播标题
     */
    private String streamTitle;
    
    /**
     * 直播开始时间戳（Unix 时间戳）
     */
    private int timestamp;
}
