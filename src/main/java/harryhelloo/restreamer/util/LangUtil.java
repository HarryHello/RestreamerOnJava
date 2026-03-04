package harryhelloo.restreamer.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 语言代码工具类
 *
 * <p>提供语言代码的转换和映射功能，用于翻译服务中的语言参数处理。</p>
 *
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>将"语言 - 地区"代码转换为纯语言代码（如 {@code en-US} → {@code en}）</li>
 *     <li>目标语言代码映射（如 {@code zh-CN} → {@code zh-HANS}）</li>
 * </ul>
 *
 * <h2>支持的目标语言映射：</h2>
 * <ul>
 *     <li>英语：{@code en-GB}, {@code en-US} → {@code en-GB}, {@code en-US}</li>
 *     <li>葡萄牙语：{@code pt-BR}, {@code pt-PT} → {@code pt-BR}, {@code pt-PT}</li>
 *     <li>中文：{@code zh-CN} → {@code zh-HANS}（简体），{@code zh-HK}, {@code zh-TW} → {@code zh-HANT}（繁体）</li>
 * </ul>
 *
 * <h2>使用示例：</h2>
 * <pre>
 * // 获取源语言代码
 * String sourceLang = LangUtil.sourceLangMapper("en-US");  // 返回 "en"
 *
 * // 获取目标语言代码
 * String targetLang = LangUtil.targetLangMapper("zh-CN");  // 返回 "zh-HANS"
 * </pre>
 *
 * @author harryhelloo
 * @version 1.0
 */
public class LangUtil {

    private static final Map<String, String> TARGET_LANG_MAP = new HashMap<>();

    static {
        TARGET_LANG_MAP.put("en-GB", "en-GB");
        TARGET_LANG_MAP.put("en-US", "en-US");
        TARGET_LANG_MAP.put("pt-BR", "pt-BR");
        TARGET_LANG_MAP.put("pt-PT", "pt-PT");
        TARGET_LANG_MAP.put("zh-CN", "zh-HANS");
        TARGET_LANG_MAP.put("zh-HANS", "zh-HANS");
        TARGET_LANG_MAP.put("zh-HANT", "zh-HANT");
        TARGET_LANG_MAP.put("zh-HK", "zh-HANT");
        TARGET_LANG_MAP.put("zh-TW", "zh-HANT");
    }

    /**
     * 将"语言 - 地区"代码（如 en-US）转换为"语言"代码（如 en）
     *
     * @param sourceLang 语言 - 地区代码，如 en-US、zh-CN、ja-JP 等
     * @return 语言代码，如 en、zh、ja 等；如果输入为 null 或空则返回 null
     */
    public static String sourceLangMapper(String sourceLang) {
        if (sourceLang == null || sourceLang.isEmpty()) {
            return null;
        }
        int separatorIndex = sourceLang.indexOf('-');
        if (separatorIndex == -1) {
            // 没有连字符，直接返回原代码（可能已经是纯语言代码）
            return sourceLang;
        }
        return sourceLang.substring(0, separatorIndex);
    }

    /**
     * 将目标语言代码映射为标准化的代码
     *
     * <p>对于中文，会将 {@code zh-CN} 映射为 {@code zh-HANS}（简体中文），
     * 将 {@code zh-HK}/{@code zh-TW} 映射为 {@code zh-HANT}（繁体中文）。</p>
     *
     * @param targetLang 目标语言代码
     * @return 标准化后的语言代码；如果输入为 null 或空则返回 null
     */
    public static String targetLangMapper(String targetLang) {
        if (targetLang == null || targetLang.isEmpty()) {
            return null;
        }

        if (TARGET_LANG_MAP.containsKey(targetLang)) {
            return TARGET_LANG_MAP.get(targetLang);
        }

        int separatorIndex = targetLang.indexOf('-');
        if (separatorIndex == -1) {
            return targetLang;
        }
        return targetLang.substring(0, separatorIndex);
    }
}
