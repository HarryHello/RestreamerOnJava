package harryhelloo.restreamer.service;

import com.openai.core.http.AsyncStreamResponse;
import harryhelloo.restreamer.pojo.Options;
import harryhelloo.restreamer.repository.translation.TranslationRepository;
import harryhelloo.restreamer.repository.translation.TranslationRepositoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TranslationService {
    
    private final TranslationRepositoryFactory repositoryFactory;
    
    @Autowired
    public TranslationService(TranslationRepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }
    
    /**
     * 初始化模型列表
     * @return 模型列表选项
     */
    public Options initModels() {
        TranslationRepository repository = repositoryFactory.getTranslationRepository();
        return repository.initModels();
    }
    
    /**
     * 翻译文本
     * @param text 要翻译的文本
     * @param sourceLang 源语言
     * @param targetLang 目标语言
     * @return 翻译结果的异步流
     */
    public AsyncStreamResponse<String> translate(String text, String sourceLang, String targetLang) {
        TranslationRepository repository = repositoryFactory.getTranslationRepository();
        return repository.translate(text, sourceLang, targetLang);
    }
    
    /**
     * 根据指定的翻译服务初始化模型列表
     * @param producer 翻译服务名称
     * @return 模型列表选项
     */
    public Options initModels(String producer) {
        TranslationRepository repository = repositoryFactory.getTranslationRepository(producer);
        return repository.initModels();
    }
    
    /**
     * 根据指定的翻译服务翻译文本
     * @param producer 翻译服务名称
     * @param text 要翻译的文本
     * @param sourceLang 源语言
     * @param targetLang 目标语言
     * @return 翻译结果的异步流
     */
    public AsyncStreamResponse<String> translate(String producer, String text, String sourceLang, String targetLang) {
        TranslationRepository repository = repositoryFactory.getTranslationRepository(producer);
        return repository.translate(text, sourceLang, targetLang);
    }
}