package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OBS WebSocket 连接配置
 * 
 * <p>用于配置与 OBS Studio 的 WebSocket 连接参数。</p>
 * 
 * <h2>配置项：</h2>
 * <ul>
 *     <li>host: OBS WebSocket 服务器地址（默认 localhost）</li>
 *     <li>port: OBS WebSocket 端口（默认 4455）</li>
 *     <li>password: OBS WebSocket 密码（可选）</li>
 * </ul>
 * 
 * <h2>OBS WebSocket 设置：</h2>
 * <p>在 OBS Studio 中：工具 → 设置 → WebSocket 服务器</p>
 * 
 * @author harryhelloo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObsWebsocket {
    
    /**
     * OBS WebSocket 服务器地址
     */
    @Builder.Default
    private String host = "localhost";
    
    /**
     * OBS WebSocket 端口
     */
    @Builder.Default
    private Integer port = 4455;
    
    /**
     * OBS WebSocket 密码（可选）
     */
    @Builder.Default
    private String password = null;
}
