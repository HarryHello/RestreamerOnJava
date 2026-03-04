package harryhelloo.restreamer.util;

import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.pojo.Settings;

import java.text.MessageFormat;

/**
 * Prompt 工具类
 * 
 * <p>生成用于 AI 翻译的提示词（Prompt）。</p>
 * 
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>生成翻译提示词，指导 AI 翻译并只返回翻译结果</li>
 * </ul>
 * 
 * <h2>Prompt 格式：</h2>
 * <pre>
 * "Please translate the follow text from {sourceLang} to {targetLang} and answer only result: {text}"
 * </pre>
 * 
 * <h2>使用示例：</h2>
 * <pre>
 * String prompt = promptUtil.translatePrompt("Hello", "en", "zh");
 * // 生成："Please translate the follow text from en to zh and answer only result: Hello"
 * </pre>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see SettingsManager
 */
public class promptUtil {
    
    /**
     * 生成翻译提示词
     * 
     * <p>生成的提示词会指导 AI 将文本从源语言翻译为目标语言，并只返回翻译结果。</p>
     *
     * @param text 待翻译的文本
     * @param sourceLang 源语言代码（如 {@code en}, {@code zh}）
     * @param targetLang 目标语言代码（如 {@code zh}, {@code ja}）
     * @return 翻译提示词
     */
    public static String translatePrompt(String text, String sourceLang, String targetLang) {
        Settings settings = SettingsManager.getInstance().getSettings();
        return MessageFormat.format(
            "Please translate the follow text from {0} to {1} and answer only result: {2}",
            sourceLang, targetLang, text
        );
    }
}
