package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 配置
 * 
 * <p>用于配置 OpenAI API 服务。</p>
 * 
 * <h2>配置项：</h2>
 * <ul>
 *     <li>baseUrl: OpenAI API 基础 URL（可使用代理）</li>
 *     <li>apiKey: OpenAI API 密钥</li>
 *     <li>model: 使用的模型名称（如 gpt-3.5-turbo、gpt-4 等）</li>
 * </ul>
 * 
 * @author harryhelloo
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenaiConfig {
    
    /**
     * OpenAI API 基础 URL（可使用代理）
     */
    private String baseUrl;
    
    /**
     * OpenAI API 密钥
     */
    private String apiKey;
    
    /**
     * 使用的模型名称（如 gpt-3.5-turbo、gpt-4 等）
     */
    private String model;
}
