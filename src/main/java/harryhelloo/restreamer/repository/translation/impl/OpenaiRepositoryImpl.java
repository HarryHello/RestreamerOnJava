package harryhelloo.restreamer.repository.translation.impl;

import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionStreamOptions;
import com.openai.models.models.ModelListPageAsync;
import harryhelloo.restreamer.exception.EmptyConfigException;
import harryhelloo.restreamer.exception.OpenaiException;
import harryhelloo.restreamer.pojo.OpenaiConfig;
import harryhelloo.restreamer.pojo.Options;
import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.repository.translation.OpenaiRepository;
import harryhelloo.restreamer.utils.ModelNameUtil;
import harryhelloo.restreamer.utils.promptUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Log4j2
@Component
public class OpenaiRepositoryImpl implements OpenaiRepository, Settings.ConfigurationChangeListener {
    @Getter
    private OpenAIClientAsync client;

    @Getter
    private boolean isReady = false;

    // 使用PostConstruct注解初始化
    @PostConstruct
    public void init() {
        // 注册配置变更监听器
        Settings.get().addConfigurationChangeListener(this);
    }

    private void closeClient() {
        if (client != null) {
            client.close();
            client = null;
        }
        isReady = false;
    }

    private void buildClient() {
        if (isReady) {
            closeClient();
        }
        OpenaiConfig openaiConfig = Settings.get().getOpenaiConfig();
        if (openaiConfig == null) {
            throw new EmptyConfigException("OpenAI config is empty!");
        }

        String baseUrl = openaiConfig.getBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new EmptyConfigException("Base url is empty!");
        }

        String apiKey = openaiConfig.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new EmptyConfigException("Api key is empty!");
        }
        this.client = OpenAIOkHttpClientAsync.builder()
            .apiKey(apiKey.trim())
            .baseUrl(baseUrl.trim())
            .build();
        isReady = true;
    }

    // 初始化客户端
    private void initializeClient() {
        OpenaiConfig openaiConfig = Settings.get().getOpenaiConfig();
        if (openaiConfig != null) {
            closeClient();
            buildClient();
        }
    }

    @Override
    public Options initModels() {
        if (client == null) {
            initializeClient();
        }

        if (client == null) {
            throw new OpenaiException("OpenAI client is not initialized. Please check your configuration.");
        }

        try {
            // 使用同一个异步客户端获取模型列表，但阻塞等待结果
            CompletableFuture<ModelListPageAsync> modelListPageFuture = client.models().list();
            ModelListPageAsync modelListPage = modelListPageFuture.join(); // 阻塞等待
            Options options = new Options();
            // 初始化options列表
            options.setOptions(new java.util.ArrayList<>());

            // 将模型ID添加到Options中，使用ModelNameUtil生成友好的标签
            modelListPage.data().forEach(model -> {
                String modelId = model.id();
                String modelLabel = ModelNameUtil.getLabel(modelId);
                options.addOption(modelLabel, modelId);
            });

            return options;
        } catch (Exception e) {
            throw new OpenaiException("Failed to get models from OpenAI", e);
        }
    }

    @Override
    public AsyncStreamResponse<String> translate(
        String text,
        String sourceLang,
        String targetLang
    ) {
        if (client == null) {
            initializeClient();
        }

        if (client == null) {
            throw new OpenaiException("OpenAI client is not initialized. Please check your configuration.");
        }

        OpenaiConfig openaiConfig = Settings.get().getOpenaiConfig();
        if (openaiConfig == null) {
            throw new OpenaiException("OpenAI configuration is not set. Please check your settings.");
        }

        String model = openaiConfig.getModel();
        if (model == null || model.isEmpty()) {
            throw new OpenaiException("OpenAI model is not configured. Please set a valid model.");
        }

        String userMessage = promptUtil.translatePrompt(text, sourceLang, targetLang);
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .addUserMessage(userMessage)
            .model(model)
            .streamOptions(ChatCompletionStreamOptions.builder().build()) // 启用流式输出
            .build();

        try {
            // 使用流式输出
            AsyncStreamResponse<ChatCompletionChunk> streamResponse =
                client.chat().completions().createStreaming(params);

            // 创建一个新的AsyncStreamResponse，将ChatCompletionChunk转换为String
            return new AsyncStreamResponse<>() {
                @Override
                public void close() {
                    streamResponse.close();
                }

                @Override
                public @NotNull CompletableFuture<Void> onCompleteFuture() {
                    return streamResponse.onCompleteFuture();
                }

                @Override
                public AsyncStreamResponse<String> subscribe(Handler<? super String> handler) {
                    streamResponse.subscribe(new Handler<>() {
                        @Override
                        public void onComplete(java.util.Optional<Throwable> error) {
                            handler.onComplete(error);
                        }

                        @Override
                        public void onNext(ChatCompletionChunk chunk) {
                            chunk.choices().forEach(choice -> {
                                choice.delta().content().ifPresent(handler::onNext);
                            });
                        }
                    });
                    return this;
                }

                @Override
                public AsyncStreamResponse<String> subscribe(Handler<? super String> handler,
                                                             java.util.concurrent.Executor executor) {
                    streamResponse.subscribe(new Handler<ChatCompletionChunk>() {
                        @Override
                        public void onComplete(java.util.Optional<Throwable> error) {
                            handler.onComplete(error);
                        }

                        @Override
                        public void onNext(ChatCompletionChunk chunk) {
                            chunk.choices().forEach(choice -> {
                                choice.delta().content().ifPresent(handler::onNext);
                            });
                        }
                    }, executor);
                    return this;
                }
            };
        } catch (Exception e) {
            throw new OpenaiException("Failed to translate with OpenAI", e);
        }
    }

    @Override
    public void onConfigurationChanged(String key, Object oldValue, Object newValue, Settings settings) {
        // 当OpenAI配置变更时重置客户端
        if ("openaiConfig".equals(key)) {
            initializeClient();
        }
    }

}