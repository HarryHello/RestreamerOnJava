package harryhelloo.restreamer.repository.translation;

import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.repository.translation.impl.DeeplRepositoryImpl;
import harryhelloo.restreamer.repository.translation.impl.OllamaRepositoryImpl;
import harryhelloo.restreamer.repository.translation.impl.OpenaiRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TranslationRepositoryFactory {

    private final Map<String, TranslationRepository> repositories = new HashMap<>();

    @Autowired
    public TranslationRepositoryFactory(
        OpenaiRepositoryImpl openaiRepositoryImpl,
        OllamaRepositoryImpl ollamaRepositoryImpl,
        DeeplRepositoryImpl deeplRepositoryImpl
    ) {
        // 注册现有的翻译服务实现
        repositories.put("openai", openaiRepositoryImpl);
        repositories.put("ollama", ollamaRepositoryImpl);
        repositories.put("deepl", deeplRepositoryImpl);
    }

    /**
     * 根据配置获取对应的翻译服务实现
     *
     * @return 翻译服务实现
     */
    public TranslationRepository getTranslationRepository() {
        String producer = SettingsManager.getInstance().getSettings().getTranslationProducer();
        return getTranslationRepository(producer);
    }

    /**
     * 根据指定的服务名称获取对应的翻译服务实现
     *
     * @param producer 服务名称
     * @return 翻译服务实现
     */
    public TranslationRepository getTranslationRepository(String producer) {
        TranslationRepository repository = repositories.get(producer);

        if (repository == null) {
            throw new IllegalStateException("No translation service implementations are available. Please check your configuration.");
        }

        // 验证对应服务的配置是否已设置
        validateConfiguration(producer);

        return repository;
    }

    /**
     * 验证指定翻译服务的配置是否完整
     *
     * @param producer 翻译服务名称
     */
    private void validateConfiguration(String producer) {
        Settings settings = SettingsManager.getInstance().getSettings();
        switch (producer.toLowerCase()) {
            case "openai" -> {
                if (settings.getOpenaiConfig() == null) {
                    throw new IllegalStateException("OpenAI service is selected but OpenAI configuration is not set. Please configure openaiConfig first.");
                }
            }
            case "ollama" -> {
                if (settings.getOllamaConfig() == null) {
                    throw new IllegalStateException("Ollama service is selected but Ollama configuration is not set. Please configure ollamaConfig first.");
                }
            }
            case "deepl" -> {
                if (settings.getDeeplAuthKey() == null || settings.getDeeplAuthKey().isEmpty()) {
                    throw new IllegalStateException("DeepL service is selected but DeepL authentication key is not set. Please configure deeplAuthKey first.");
                }
            }
            default -> {
                // 未知的翻译服务，由后续调用处理
            }
        }
    }

    /**
     * 注册新的翻译服务实现
     *
     * @param producer   服务名称
     * @param repository 翻译服务实现
     */
    public void registerRepository(String producer, TranslationRepository repository) {
        repositories.put(producer, repository);
    }
}
