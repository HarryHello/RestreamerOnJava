package harryhelloo.restreamer.repository.translation.impl;

import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionStreamOptions;
import com.openai.models.models.ModelListPageAsync;
import harryhelloo.restreamer.exception.OpenaiException;
import harryhelloo.restreamer.pojo.OpenaiConfig;
import harryhelloo.restreamer.pojo.Options;
import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.repository.translation.OpenaiRepository;
import harryhelloo.restreamer.utils.ModelNameUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

public class OpenaiRepositoryImpl implements OpenaiRepository, Settings.ConfigurationChangeListener {
    @Getter
    private OpenAIClientAsync client;

    // 使用PostConstruct注解初始化
    @PostConstruct
    public void init() {
        initializeClient();
        // 注册配置变更监听器
        Settings.get().addConfigurationChangeListener(this);
    }

    // 初始化客户端
    private void initializeClient() {
        OpenaiConfig openaiConfig = Settings.get().getOpenaiConfig();
        if (openaiConfig != null) {
            if (client != null) {
                // 关闭旧客户端
                client.close();
            }
            this.client = OpenAIOkHttpClientAsync.builder()
                .apiKey(openaiConfig.getApiKey())
                .baseUrl(openaiConfig.getBaseUrl())
                .build();
        }
    }

    @Override
    public Options initModels() {
        if (client == null) {
            initializeClient();
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

        OpenaiConfig openaiConfig = Settings.get().getOpenaiConfig();

        String userMessage = MessageFormat.format(
            "Please translate the follow text from {0} to {1} and answer only result: {2}",
            sourceLang, targetLang, text
        );
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .addUserMessage(userMessage)
            .model(openaiConfig.getModel())
            .streamOptions(ChatCompletionStreamOptions.builder().build()) // 启用流式输出
            .build();

        try {
            // 使用流式输出
            AsyncStreamResponse<ChatCompletionChunk> streamResponse =
                client.chat().completions().createStreaming(params);

            // 创建一个新的AsyncStreamResponse，将ChatCompletionChunk转换为String
            return new AsyncStreamResponse<String>() {
                @Override
                public AsyncStreamResponse<String> subscribe(Handler<String> handler) {
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
                public AsyncStreamResponse<String> subscribe(Handler<String> handler,
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

                @Override
                public void close() {
                    streamResponse.close();
                }

                @Override
                public CompletableFuture<Void> onCompleteFuture() {
                    return streamResponse.onCompleteFuture();
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