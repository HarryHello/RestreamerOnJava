package harryhelloo.restreamer.repository.translation.impl;

import com.openai.core.http.AsyncStreamResponse;
import harryhelloo.restreamer.exception.EmptyConfigException;
import harryhelloo.restreamer.exception.OllamaException;
import harryhelloo.restreamer.pojo.OllamaConfig;
import harryhelloo.restreamer.pojo.Options;
import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.repository.translation.OllamaRepository;
import harryhelloo.restreamer.utils.promptUtil;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.request.ThinkMode;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.models.response.OllamaAsyncResultStreamer;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Log4j2
@Component
public class OllamaRepositoryImpl implements OllamaRepository, Settings.ConfigurationChangeListener {
    private Ollama client;

    private boolean isReady() {
        if (client == null) {
            return false;
        }

        try {
            return client.ping();
        } catch (io.github.ollama4j.exceptions.OllamaException e) {
            throw new OllamaException(e);
        }
    }

    private void buildClient() {
        OllamaConfig ollamaConfig = Settings.get().getOllamaConfig();
        if (ollamaConfig == null) {
            throw new EmptyConfigException("Ollama Config is empty!");
        }

        String host = ollamaConfig.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new EmptyConfigException("Host is empty!");
        }

        Integer port = ollamaConfig.getPort();
        if (port == null) {
            throw new EmptyConfigException("Port is empty!");
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Illegal port number!");
        }

        String url = "http://%s:%d".formatted(host.trim(), port);
        client = new Ollama(url);
    }

    @Override
    public Options initModels() {
        checkConnection();

        Options options = new Options();

        try {
            List<Model> models = client.listModels();
            models.forEach(model -> options.addOption(model.getModelName(), model.getModelName()));
        } catch (io.github.ollama4j.exceptions.OllamaException e) {
            throw new OllamaException(e);
        }

        return options;
    }

    @Override
    public AsyncStreamResponse<String> translate(String text, String sourceLang, String targetLang) {
        checkConnection();
        Settings settings = Settings.get();
        OllamaConfig ollamaConfig = settings.getOllamaConfig();

        String model = ollamaConfig.getModel();
        String prompt = promptUtil.translatePrompt(text, sourceLang, targetLang);
        boolean raw = false;

        try {
            OllamaAsyncResultStreamer resultStreamer = client.generateAsync(model, prompt, raw, ThinkMode.DISABLED);
            resultStreamer.start();

            return new AsyncStreamResponse<>() {
                private final CompletableFuture<Void> completeFuture = new CompletableFuture<>();
                private boolean closed = false;

                @Override
                public void close() {
                    closed = true;
                    resultStreamer.interrupt();
                }

                @Override
                public @NotNull CompletableFuture<Void> onCompleteFuture() {
                    return completeFuture;
                }

                @Override
                public @NonNull AsyncStreamResponse<String> subscribe(@NonNull Handler<? super String> handler) {
                    startStreamingThread(handler, null);
                    return this;
                }

                @Override
                public @NonNull AsyncStreamResponse<String> subscribe(@NonNull Handler<? super String> handler,
                                                                      java.util.concurrent.@NonNull Executor executor) {
                    startStreamingThread(handler, executor);
                    return this;
                }

                private void startStreamingThread(Handler<? super String> handler, java.util.concurrent.Executor executor) {
                    Runnable runnable = () -> {
                        try {
                            while (!closed && resultStreamer.isAlive()) {
                                String chunk = resultStreamer.getResponseStream().poll();
                                if (chunk != null && !chunk.isEmpty()) {
                                    if (executor != null) {
                                        executor.execute(() -> handler.onNext(chunk));
                                    } else {
                                        handler.onNext(chunk);
                                    }
                                } else {
                                    Thread.sleep(10); // 短暂休眠，避免CPU占用过高
                                }
                            }

                            // 检查是否还有剩余数据
                            String remaining = resultStreamer.getResponseStream().poll();
                            if (remaining != null && !remaining.isEmpty()) {
                                if (executor != null) {
                                    executor.execute(() -> handler.onNext(remaining));
                                } else {
                                    handler.onNext(remaining);
                                }
                            }

                            // 完成流
                            if (!completeFuture.isDone()) {
                                if (resultStreamer.isSucceeded()) {
                                    completeFuture.complete(null);
                                    if (executor != null) {
                                        executor.execute(() -> handler.onComplete(java.util.Optional.empty()));
                                    } else {
                                        handler.onComplete(java.util.Optional.empty());
                                    }
                                } else {
                                    Exception ex = new OllamaException(resultStreamer.getCompleteResponse());
                                    completeFuture.completeExceptionally(ex);
                                    if (executor != null) {
                                        executor.execute(() -> handler.onComplete(java.util.Optional.of(ex)));
                                    } else {
                                        handler.onComplete(java.util.Optional.of(ex));
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            Exception ex = new OllamaException("Streaming interrupted");
                            if (!completeFuture.isDone()) {
                                completeFuture.completeExceptionally(ex);
                                if (executor != null) {
                                    executor.execute(() -> handler.onComplete(java.util.Optional.of(ex)));
                                } else {
                                    handler.onComplete(java.util.Optional.of(ex));
                                }
                            }
                        } catch (Exception e) {
                            Exception ex = new OllamaException("Streaming failed: " + e.getMessage(), e);
                            if (!completeFuture.isDone()) {
                                completeFuture.completeExceptionally(ex);
                                if (executor != null) {
                                    executor.execute(() -> handler.onComplete(java.util.Optional.of(ex)));
                                } else {
                                    handler.onComplete(java.util.Optional.of(ex));
                                }
                            }
                        }
                    };

                    if (executor != null) {
                        executor.execute(runnable);
                    } else {
                        new Thread(runnable).start();
                    }
                }
            };
        } catch (io.github.ollama4j.exceptions.OllamaException e) {
            throw new OllamaException(e);
        }
    }

    private void checkConnection() {
        if (client == null) {
            buildClient();
        }
        if (!isReady()) {
            throw new OllamaException("Failed to connect to Ollama!");
        }
    }

    @Override
    public void onConfigurationChanged(String key, Object oldValue, Object newValue, Settings settings) {

    }
}