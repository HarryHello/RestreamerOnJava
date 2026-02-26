package harryhelloo.restreamer.utils;

import java.util.HashMap;
import java.util.Map;

public class ModelNameUtil {
    // 常见模型的映射表，用于特殊处理
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

    public static String getLabel(String modelName) {
        // 首先检查是否有特殊映射
        if (MODEL_LABEL_MAP.containsKey(modelName)) {
            return MODEL_LABEL_MAP.get(modelName);
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

    // 批量处理模型名称列表
    public static Map<String, String> getLabelMap(Iterable<String> modelNames) {
        Map<String, String> labelMap = new HashMap<>();
        for (String modelName : modelNames) {
            labelMap.put(modelName, getLabel(modelName));
        }
        return labelMap;
    }
}