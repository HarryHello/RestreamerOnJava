package harryhelloo.restreamer.repository.translation.impl;

import com.deepl.api.*;
import com.openai.core.http.AsyncStreamResponse;
import harryhelloo.restreamer.exception.DeeplException;
import harryhelloo.restreamer.exception.EmptyConfigException;
import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.pojo.Options;
import harryhelloo.restreamer.repository.translation.DeeplRepository;
import harryhelloo.restreamer.util.LangUtil;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Log4j2
@Component
public class DeeplRepositoryImpl implements DeeplRepository {
    private DeepLClient client;

    private void buildClient() {
        String authKey = SettingsManager.getInstance().getSettings().getDeeplAuthKey();
        if (authKey == null) {
            throw new EmptyConfigException("DeepL AuthKey is empty");
        }
        this.client = new DeepLClient(authKey);
    }

    public boolean isReady() {
        if (client == null) {
            return false;
        }
        try {
            Usage usage = client.getUsage();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Options initModels() {
        if (client == null) {
            buildClient();
        }

        if (!isReady()) {
            throw new DeeplException("Failed to connect to DeepL!");
        }

        try {
            Usage usage = client.getUsage();
        } catch (AuthorizationException e) {
            throw new RuntimeException(e);
        } catch (DeepLException e) {
            throw new DeeplException(e);
        } catch (InterruptedException e) {
            throw new DeeplException("DeepL test was interrupted!", e);
        }
        return new Options();
    }

    @Override
    public AsyncStreamResponse<String> translate(String text, String sourceLang, String targetLang) {
        if (client == null) {
            buildClient();
        }

        if (!isReady()) {
            throw new DeeplException("Failed to connect to DeepL!");
        }

        // 使用 LangUtil.toLanguageCode 转换语言代码
        String source = LangUtil.sourceLangMapper(sourceLang);
        String target = LangUtil.targetLangMapper(targetLang);

        // 构建 DeepL 语言代码（DeepL 使用特定的语言代码格式，如 EN、ZH 等）
        String sourceLangCode = source != null ? source.toUpperCase() : null;
        String targetLangCode = target != null ? target.toUpperCase() : "";

        return new AsyncStreamResponse<>() {
            private final CompletableFuture<Void> completeFuture = new CompletableFuture<>();
            private boolean closed = false;

            @Override
            public void close() {
                closed = true;
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

            private void startStreamingThread(Handler<? super String> handler,
                                              java.util.concurrent.Executor executor) {
                Runnable runnable = () -> {
                    try {
                        if (closed) {
                            return;
                        }

                        // 执行 DeepL 翻译（同步调用）
                        TextResult result = client.translateText(text, sourceLangCode, targetLangCode);
                        String translatedText = result.getText();

                        // 将整个翻译结果作为一个 chunk 发送
                        if (executor != null) {
                            executor.execute(() -> handler.onNext(translatedText));
                        } else {
                            handler.onNext(translatedText);
                        }

                        // 完成流
                        if (!completeFuture.isDone()) {
                            completeFuture.complete(null);
                            if (executor != null) {
                                executor.execute(() -> handler.onComplete(java.util.Optional.empty()));
                            } else {
                                handler.onComplete(java.util.Optional.empty());
                            }
                        }
                    } catch (DeepLException e) {
                        Exception ex = new DeeplException(e);
                        if (!completeFuture.isDone()) {
                            completeFuture.completeExceptionally(ex);
                            if (executor != null) {
                                executor.execute(() -> handler.onComplete(java.util.Optional.of(ex)));
                            } else {
                                handler.onComplete(java.util.Optional.of(ex));
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Exception ex = new DeeplException("Translation with DeepL was interrupted!", e);
                        if (!completeFuture.isDone()) {
                            completeFuture.completeExceptionally(ex);
                            if (executor != null) {
                                executor.execute(() -> handler.onComplete(java.util.Optional.of(ex)));
                            } else {
                                handler.onComplete(java.util.Optional.of(ex));
                            }
                        }
                    } catch (Exception e) {
                        Exception ex = new DeeplException("Translation failed: " + e.getMessage(), e);
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
    }
}
