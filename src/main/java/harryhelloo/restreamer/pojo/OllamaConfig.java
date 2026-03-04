package harryhelloo.restreamer.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ollama 配置
 * 
 * <p>用于配置 Ollama 本地大语言模型服务。</p>
 * 
 * <h2>配置项：</h2>
 * <ul>
 *     <li>host: Ollama 服务器地址</li>
 *     <li>port: Ollama 服务端口（默认 11434）</li>
 *     <li>model: 使用的模型名称（如 llama2、qwen2 等）</li>
 * </ul>
 * 
 * @author harryhelloo
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaConfig {
    
    /**
     * Ollama 服务器地址
     */
    private String host;
    
    /**
     * Ollama 服务端口
     */
    private Integer port;
    
    /**
     * 使用的模型名称（如 llama2、qwen2 等）
     */
    private String model;
}
