package harryhelloo.restreamer.controller;

import com.openai.core.http.AsyncStreamResponse;
import harryhelloo.restreamer.service.ChannelService;
import harryhelloo.restreamer.service.SubtitleService;
import harryhelloo.restreamer.service.TranslationService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 翻译控制器
 * 
 * <p>提供文本翻译服务，支持流式翻译（SSE）和字幕记录集成功能。</p>
 * 
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>翻译模型列表获取</li>
 *     <li>流式翻译（Server-Sent Events）</li>
 *     <li>单次翻译</li>
 *     <li>翻译会话管理（开始/结束）</li>
 * </ul>
 * 
 * <h2>API 端点：</h2>
 * <ul>
 *     <li>{@code GET /api/translation/models} - 获取可用翻译模型列表</li>
 *     <li>{@code POST /api/translation/translate/stream} - 流式翻译（SSE）</li>
 *     <li>{@code POST /api/translation/translate} - 单次翻译</li>
 *     <li>{@code POST /api/translation/session/start} - 开始翻译会话</li>
 *     <li>{@code POST /api/translation/session/end} - 结束翻译会话</li>
 * </ul>
 * 
 * <h2>注意事项：</h2>
 * <p>字幕录制功能已迁移至 {@link SubtitleController}，建议使用 {@code /api/subtitles/process} 统一 API。</p>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see SubtitleController
 */
@Log4j2
@RestController
@RequestMapping("/api/translation")
public class TranslationController {
    
    @Autowired
    private TranslationService translationService;

    @Autowired
    private SubtitleService subtitleService;

    @Autowired
    private ChannelService channelService;

    /**
     * 正在翻译的会话（channelId -> 是否记录字幕）
     * <p>用于跟踪哪些翻译会话需要记录字幕</p>
     */
    private final Map<String, Boolean> recordingSessions = new ConcurrentHashMap<>();

    /**
     * 初始化模型列表
     * 
     * <p>获取当前配置的翻译服务支持的模型列表。</p>
     * 
     * @return 模型列表选项
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> getModels() {
        try {
            var options = translationService.initModels();
            return ResponseEntity.ok(options.toMap());
        } catch (Exception e) {
            log.error("Failed to get models", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 流式翻译（SSE）
     * 
     * <p>提供流式翻译服务，通过 Server-Sent Events 实时返回翻译结果。</p>
     * 
     * <h3>功能说明：</h3>
     * <ul>
     *     <li>支持边翻译边返回结果（流式）</li>
     *     <li>可选是否记录字幕（通过 recordSubtitle 参数）</li>
     *     <li>翻译完成后自动记录原文和翻译字幕</li>
     * </ul>
     * 
     * <h3>注意：</h3>
     * <p>此方法使用估算的时间戳记录字幕。如需精确时间控制，建议使用 {@link SubtitleController#processSubtitle}。</p>
     * 
     * @param request 请求参数
     *                - text: 待翻译文本
     *                - from: 源语言代码
     *                - to: 目标语言代码
     * @param channelId 频道 ID（可选，用于字幕记录）
     * @param recordSubtitle 是否记录字幕（可选，默认 false）
     * @return SSE Emitter，推送翻译结果
     */
    @PostMapping(value = "/translate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter translateStream(
        @RequestBody Map<String, String> request,
        @RequestParam(value = "channelId", required = false) String channelId,
        @RequestParam(value = "recordSubtitle", required = false) Boolean recordSubtitle
    ) {
        String text = request.get("text");
        String sourceLang = request.get("from");
        String targetLang = request.get("to");

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        try {
            // 检查是否需要记录字幕
            boolean shouldRecord = recordSubtitle != null && recordSubtitle && channelId != null && !channelId.isEmpty();

            if (shouldRecord) {
                recordingSessions.put(channelId, true);
                log.info("Subtitle recording enabled for translation session: {}", channelId);
            }

            // 执行流式翻译
            AsyncStreamResponse<String> streamResponse = translationService.translate(text, sourceLang, targetLang);

            // 用于累积翻译结果
            StringBuilder translatedText = new StringBuilder();
            final long[] startTime = {System.currentTimeMillis()};

            streamResponse.subscribe(new AsyncStreamResponse.Handler<String>() {
                @Override
                public void onNext(String chunk) {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("translation")
                            .data(chunk));

                        // 累积翻译结果
                        translatedText.append(chunk);

                    } catch (IOException e) {
                        log.error("Failed to send translation chunk", e);
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onComplete(java.util.Optional<Throwable> error) {
                    if (error.isPresent()) {
                        log.error("Translation error", error.get());
                        emitter.completeWithError(error.get());
                    } else {
                        log.debug("Translation completed");

                        // 记录字幕（如果启用）
                        // 注意：原文字幕已经在 /api/subtitles/new 中记录（带精确时间）
                        // 这里只记录翻译字幕，使用缓存的源字幕时间
                        if (shouldRecord && channelId != null) {
                            try {
                                recordTranslateSubtitle(channelId, text, translatedText.toString());
                            } catch (RuntimeException e) {
                                log.warn("Subtitle recording failed, but translation succeeded: {}", e.getMessage());
                                // 不抛出，因为翻译已经成功
                            }
                        }

                        try {
                            // 发送完成事件
                            emitter.send(SseEmitter.event()
                                .name("complete")
                                .data("{\"status\":\"completed\"}"));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("Failed to send completion event", e);
                        }
                    }

                    // 清理会话
                    if (channelId != null) {
                        recordingSessions.remove(channelId);
                    }
                }
            });

        } catch (Exception e) {
            log.error("Translation error", e);
            emitter.completeWithError(e);
            if (channelId != null) {
                recordingSessions.remove(channelId);
            }
        }

        // 处理客户端断开连接
        emitter.onTimeout(() -> {
            log.debug("SSE timeout for channel: {}", channelId);
            if (channelId != null) {
                recordingSessions.remove(channelId);
            }
        });

        emitter.onError(e -> {
            log.error("SSE error for channel: {}", channelId, e);
            if (channelId != null) {
                recordingSessions.remove(channelId);
            }
        });

        return emitter;
    }
    
    /**
     * 单次翻译（不记录字幕）
     * 
     * <p>执行一次性翻译，返回完整翻译结果。</p>
     * 
     * @param request 请求参数
     *                - text: 待翻译文本
     *                - from: 源语言代码
     *                - to: 目标语言代码
     * @return 翻译后的文本
     */
    @PostMapping("/translate")
    public ResponseEntity<String> translate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String sourceLang = request.get("from");
        String targetLang = request.get("to");
        
        try {
            // 使用流式翻译并累积结果
            AsyncStreamResponse<String> streamResponse = translationService.translate(text, sourceLang, targetLang);
            StringBuilder result = new StringBuilder();
            
            streamResponse.subscribe(new AsyncStreamResponse.Handler<String>() {
                @Override
                public void onNext(String chunk) {
                    result.append(chunk);
                }
                
                @Override
                public void onComplete(java.util.Optional<Throwable> error) {
                    // 同步完成（简单实现）
                }
            });
            
            // 等待完成（简单实现，实际应该用 CompletableFuture）
            Thread.sleep(100);
            
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            log.error("Translation error", e);
            return ResponseEntity.internalServerError().body("Translation failed: " + e.getMessage());
        }
    }
    
    /**
     * 开始字幕录制会话
     * 
     * <p>初始化字幕录制环境，创建 SRT 文件写入器。</p>
     * 
     * @param request 请求参数，包含 channelId
     * @return 会话启动结果
     * 
     * @deprecated 已迁移至 {@link SubtitleController#startSession}
     */
    @Deprecated(since = "2026-03-01", forRemoval = true)
    @PostMapping("/session/start")
    public ResponseEntity<String> startSession(@RequestBody Map<String, String> request) {
        try {
            String channelId = request.get("channelId");

            if (channelId == null || channelId.isEmpty()) {
                return ResponseEntity.badRequest().body("channelId is required");
            }

            // 通过 ChannelService 获取频道信息
            harryhelloo.restreamer.pojo.StreamerChannel channel = channelService.getChannel(channelId);
            if (channel == null) {
                return ResponseEntity.badRequest().body("Channel not found: " + channelId);
            }

            // 开始录制字幕
            subtitleService.startRecording(channel);
            recordingSessions.put(channelId, true);

            log.info("Translation session started for channel: {}", channelId);
            return ResponseEntity.ok("Session started");
        } catch (Exception e) {
            log.error("Failed to start session", e);
            return ResponseEntity.internalServerError().body("Failed to start session: " + e.getMessage());
        }
    }
    
    /**
     * 结束字幕录制会话
     * 
     * <p>关闭字幕录制环境，清理资源。</p>
     * 
     * @param request 请求参数，包含 channelId
     * @return 会话结束结果
     * 
     * @deprecated 已迁移至 {@link SubtitleController#endSession}
     */
    @Deprecated(since = "2026-03-01", forRemoval = true)
    @PostMapping("/session/end")
    public ResponseEntity<String> endSession(@RequestBody Map<String, String> request) {
        try {
            String channelId = request.get("channelId");
            
            if (channelId == null || channelId.isEmpty()) {
                return ResponseEntity.badRequest().body("channelId is required");
            }
            
            // 停止录制字幕
            subtitleService.stopRecording(channelId);
            recordingSessions.remove(channelId);
            
            log.info("Translation session ended for channel: {}", channelId);
            return ResponseEntity.ok("Session ended");
        } catch (Exception e) {
            log.error("Failed to end session", e);
            return ResponseEntity.internalServerError().body("Failed to end session: " + e.getMessage());
        }
    }
    
    /**
     * 记录翻译字幕（使用缓存的源字幕时间，保证时间同步）
     * 
     * <p>注意：原文字幕已经在 /api/subtitles/new 中记录。</p>
     *
     * @param channelId 频道 ID
     * @param sourceText 原文文本
     * @param translatedText 翻译后的文本
     * @throws RuntimeException 当字幕记录失败时抛出
     *
     * @see SubtitleService#writeTranslateWithTime
     */
    private void recordTranslateSubtitle(String channelId, String sourceText, String translatedText) {
        if (!recordingSessions.getOrDefault(channelId, false)) {
            return;
        }

        try {
            // 使用缓存机制，翻译字幕会与源字幕使用相同的时间戳
            subtitleService.writeTranslateWithTime(channelId, sourceText, translatedText);
            log.debug("Translate subtitle recorded for channel: {}", channelId);
            
        } catch (IllegalStateException e) {
            // 字幕服务未初始化，这是可恢复的错误
            log.warn("Cannot record translate subtitle for channel: {}. Subtitle service not ready.", channelId);
            // 不抛出，因为翻译本身成功了
        } catch (Exception e) {
            log.error("Failed to record translate subtitle for channel: {}", channelId, e);
            // 抛出运行时异常，让调用者知道
            throw new RuntimeException("Failed to record subtitle: " + e.getMessage(), e);
        }
    }
}
