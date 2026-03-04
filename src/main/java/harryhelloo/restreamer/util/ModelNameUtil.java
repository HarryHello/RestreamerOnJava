package harryhelloo.restreamer.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型名称工具类
 * 
 * <p>将模型名称转换为人类可读的标签，用于前端显示。</p>
 * 
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>特殊模型映射（如 {@code gpt-4o} → {@code GPT-4o}）</li>
 *     <li>通用模型名称格式化（如 {@code llama3-8b} → {@code LLaMA 3 8B}）</li>
 *     <li>批量处理模型名称列表</li>
 * </ul>
 * 
 * <h2>支持的模型系列：</h2>
 * <ul>
 *     <li><strong>OpenAI：</strong>GPT-4o, GPT-4 Turbo, GPT-3.5 Turbo</li>
 *     <li><strong>Anthropic：</strong>Claude 3 Opus/Sonnet/Haiku</li>
 *     <li><strong>Google：</strong>Gemini 1.5 Pro/Flash</li>
 *     <li><strong>Meta：</strong>LLaMA 3</li>
 *     <li><strong>Mistral：</strong>Mistral 7B, Mixtral 8x7B</li>
 *     <li><strong>其他：</strong>Qwen, DeepSeek, Kimi, MiniMax, GLM, Falcon, BLOOM</li>
 * </ul>
 * 
 * <h2>使用示例：</h2>
 * <pre>
 * // 单个模型名称转换
 * String label = ModelNameUtil.getLabel("gpt-4o");  // 返回 "GPT-4o"
 * 
 * // 批量处理
 * Map&lt;String, String&gt; labelMap = ModelNameUtil.getLabelMap(modelNames);
 * </pre>
 * 
 * @author harryhelloo
 * @version 1.0
 */
public class ModelNameUtil {
    
    /**
     * 常见模型的映射表，用于特殊处理
     */
    private static final Map<String, String> MODEL_LABEL_MAP = new HashMap<>();

    static {
        // OpenAI 模型
        MODEL_LABEL_MAP.put("gpt-4o", "GPT-4o");
        MODEL_LABEL_MAP.put("gpt-4-turbo", "GPT-4 Turbo");
        MODEL_LABEL_MAP.put("gpt-4", "GPT-4");
        MODEL_LABEL_MAP.put("gpt-3.5-turbo", "GPT-3.5 Turbo");

        // Anthropic 模型
        MODEL_LABEL_MAP.put("claude-3-opus-20240229", "Claude 3 Opus");
        MODEL_LABEL_MAP.put("claude-3-sonnet-20240229", "Claude 3 Sonnet");
        MODEL_LABEL_MAP.put("claude-3-haiku-20240307", "Claude 3 Haiku");

        // Google 模型
        MODEL_LABEL_MAP.put("gemini-1.5-pro", "Gemini 1.5 Pro");
        MODEL_LABEL_MAP.put("gemini-1.5-flash", "Gemini 1.5 Flash");
        MODEL_LABEL_MAP.put("gemini-1.0-pro", "Gemini 1.0 Pro");

        // 其他常见模型
        MODEL_LABEL_MAP.put("llama3-8b", "LLaMA 3 8B");
        MODEL_LABEL_MAP.put("llama3-70b", "LLaMA 3 70B");
        MODEL_LABEL_MAP.put("mistral-7b-v0.1", "Mistral 7B v0.1");
        MODEL_LABEL_MAP.put("mistral-7b-v0.2", "Mistral 7B v0.2");
        MODEL_LABEL_MAP.put("mixtral-8x7b-v0.1", "Mixtral 8x7B v0.1");
    }

    /**
     * 将模型名称转换为人类可读的标签
     * 
     * <p>首先检查特殊映射表，如果没有则使用通用规则转换。</p>
     *
     * @param modelName 模型名称（如 {@code gpt-4o}, {@code llama3-8b}）
     * @return 人类可读的标签（如 {@code GPT-4o}, {@code LLaMA 3 8B}）
     */
    public static String getLabel(String modelName) {
        // 首先检查是否有特殊映射
        if (MODEL_LABEL_MAP.containsKey(modelName)) {
            return MODEL_LABEL_MAP.get(modelName);
        }

        if (modelName.contains("/")) {
            return modelName;
        }

        // 通用转换逻辑
        String label = modelName
            // 处理常见模型前缀
            .replaceAll("gpt", "GPT")
            .replaceAll("qwen", "Qwen")
            .replaceAll("deepseek", "DeepSeek")
            .replaceAll("kimi", "Kimi")
            .replaceAll("minimax", "MiniMax")
            .replaceAll("glm", "GLM")
            .replaceAll("claude", "Claude")
            .replaceAll("gemini", "Gemini")
            .replaceAll("llama", "LLaMA")
            .replaceAll("mistral", "Mistral")
            .replaceAll("mixtral", "Mixtral")
            .replaceAll("falcon", "Falcon")
            .replaceAll("bloom", "BLOOM")
            // 处理后缀
            .replaceAll("-turbo", " Turbo")
            .replaceAll("-max", " Max")
            .replaceAll("-pro", " Pro")
            .replaceAll("-plus", " Plus")
            .replaceAll("-flash", " Flash")
            .replaceAll("-opus", " Opus")
            .replaceAll("-sonnet", " Sonnet")
            .replaceAll("-haiku", " Haiku")
            // 处理版本号
            .replaceAll("-v([0-9]+(\\.[0-9]+)*)", " v$1");

        // 处理数字和字母之间的连接
        label = label.replaceAll("([a-zA-Z])([0-9])", "$1 $2");

        return label;
    }

    /**
     * 批量处理模型名称列表
     * 
     * @param modelNames 模型名称集合
     * @return 模型名称到标签的映射表
     */
    public static Map<String, String> getLabelMap(Iterable<String> modelNames) {
        Map<String, String> labelMap = new HashMap<>();
        for (String modelName : modelNames) {
            labelMap.put(modelName, getLabel(modelName));
        }
        return labelMap;
    }
}
