package harryhelloo.restreamer.service;

import com.openai.core.http.AsyncStreamResponse;
import harryhelloo.restreamer.pojo.Options;
import harryhelloo.restreamer.repository.translation.TranslationRepository;
import harryhelloo.restreamer.repository.translation.TranslationRepositoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 翻译服务
 * 
 * <p>提供文本翻译功能，支持多种翻译后端（Ollama、OpenAI、DeepL）。</p>
 * 
 * <h2>支持的翻译服务：</h2>
 * <ul>
 *     <li><strong>Ollama：</strong>本地部署的开源大语言模型</li>
 *     <li><strong>OpenAI：</strong>GPT 系列模型</li>
 *     <li><strong>DeepL：</strong>专业翻译 API</li>
 * </ul>
 * 
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>流式翻译（Server-Sent Events）</li>
 *     <li>模型列表获取</li>
 *     <li>指定翻译服务</li>
 * </ul>
 * 
 * <h2>使用示例：</h2>
 * <pre>
 * // 流式翻译
 * AsyncStreamResponse&lt;String&gt; response = translationService.translate(
 *     "Hello World", "en", "zh");
 * 
 * // 获取模型列表
 * Options models = translationService.initModels();
 * </pre>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see TranslationRepositoryFactory
 * @see TranslationRepository
 */
@Service
public class TranslationService {
    
    private final TranslationRepositoryFactory repositoryFactory;

    /**
     * 构造注入翻译仓库工厂
     * 
     * @param repositoryFactory 翻译仓库工厂
     */
    @Autowired
    public TranslationService(TranslationRepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    /**
     * 初始化模型列表
     * 
     * <p>使用默认翻译服务初始化可用模型列表。</p>
     * 
     * @return 模型列表选项
     */
    public Options initModels() {
        TranslationRepository repository = repositoryFactory.getTranslationRepository();
        return repository.initModels();
    }
    
    /**
     * 翻译文本
     * 
     * <p>使用默认翻译服务进行流式翻译。</p>
     * 
     * @param text 要翻译的文本
     * @param sourceLang 源语言代码（如 "en"、"zh"）
     * @param targetLang 目标语言代码（如 "zh"、"ja"）
     * @return 翻译结果的异步流，可通过 subscribe 监听翻译过程
     */
    public AsyncStreamResponse<String> translate(String text, String sourceLang, String targetLang) {
        TranslationRepository repository = repositoryFactory.getTranslationRepository();
        return repository.translate(text, sourceLang, targetLang);
    }
    
    /**
     * 根据指定的翻译服务初始化模型列表
     * 
     * @param producer 翻译服务名称（"ollama" / "openai" / "deepl"）
     * @return 模型列表选项
     */
    public Options initModels(String producer) {
        TranslationRepository repository = repositoryFactory.getTranslationRepository(producer);
        return repository.initModels();
    }

    /**
     * 根据指定的翻译服务翻译文本
     * 
     * @param producer 翻译服务名称（"ollama" / "openai" / "deepl"）
     * @param text 要翻译的文本
     * @param sourceLang 源语言代码
     * @param targetLang 目标语言代码
     * @return 翻译结果的异步流
     */
    public AsyncStreamResponse<String> translate(String producer, String text, String sourceLang, String targetLang) {
        TranslationRepository repository = repositoryFactory.getTranslationRepository(producer);
        return repository.translate(text, sourceLang, targetLang);
    }
}