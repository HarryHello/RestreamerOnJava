package harryhelloo.restreamer.repository.translation;

import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.repository.translation.impl.OpenaiRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TranslationRepositoryFactory {
    
    private final Map<String, TranslationRepository> repositories = new HashMap<>();
    
    @Autowired
    public TranslationRepositoryFactory(OpenaiRepositoryImpl openaiRepositoryImpl) {
        // 注册现有的翻译服务实现
        repositories.put("openai", openaiRepositoryImpl);
        
        // 注：当实现OllamaRepositoryImpl和DeeplRepositoryImpl后，在此处注册
        // repositories.put("ollama", ollamaRepositoryImpl);
        // repositories.put("deepl", deeplRepositoryImpl);
    }
    
    /**
     * 根据配置获取对应的翻译服务实现
     * @return 翻译服务实现
     */
    public TranslationRepository getTranslationRepository() {
        String producer = Settings.get().getTranslationProducer();
        TranslationRepository repository = repositories.get(producer);
        
        if (repository == null) {
            // 如果配置的服务不可用，尝试返回默认实现（这里使用openai作为默认）
            repository = repositories.get("openai");
            
            if (repository == null) {
                throw new IllegalStateException("No translation service implementations are available. Please check your configuration.");
            }
        }
        
        return repository;
    }
    
    /**
     * 根据指定的服务名称获取对应的翻译服务实现
     * @param producer 服务名称
     * @return 翻译服务实现
     */
    public TranslationRepository getTranslationRepository(String producer) {
        TranslationRepository repository = repositories.get(producer);
        
        if (repository == null) {
            // 如果指定的服务不可用，尝试返回默认实现
            repository = repositories.get("openai");
            
            if (repository == null) {
                throw new IllegalStateException("No translation service implementations are available. Please check your configuration.");
            }
        }
        
        return repository;
    }
    
    /**
     * 注册新的翻译服务实现
     * @param producer 服务名称
     * @param repository 翻译服务实现
     */
    public void registerRepository(String producer, TranslationRepository repository) {
        repositories.put(producer, repository);
    }
}