package harryhelloo.restreamer.controller;

import com.openai.core.http.AsyncStreamResponse;
import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.pojo.SubtitleData;
import harryhelloo.restreamer.service.ChannelService;
import harryhelloo.restreamer.service.SubtitleService;
import harryhelloo.restreamer.service.TranslationService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字幕管理控制器
 *
 * <p>提供字幕文件的 CRUD 操作、字幕处理流程控制以及会话管理功能。</p>
 *
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>字幕文件管理（列表、查看、下载、删除）</li>
 *     <li>字幕筛选（按类型、按频道名称）</li>
 *     <li>统一字幕处理 API（识别 + 翻译）</li>
 *     <li>字幕录制会话管理</li>
 * </ul>
 *
 * <h2>API 端点：</h2>
 * <ul>
 *     <li>{@code GET /api/subtitles} - 获取字幕文件列表</li>
 *     <li>{@code GET /api/subtitles/{filename}} - 获取字幕文件内容</li>
 *     <li>{@code GET /api/subtitles/{filename}/download} - 下载字幕文件</li>
 *     <li>{@code DELETE /api/subtitles/{filename}} - 删除字幕文件</li>
 *     <li>{@code POST /api/subtitles/batch-delete} - 批量删除字幕文件</li>
 *     <li>{@code GET /api/subtitles/filter/{type}} - 按类型筛选字幕</li>
 *     <li>{@code GET /api/subtitles/channel/{channelName}} - 按频道名称筛选</li>
 *     <li>{@code POST /api/subtitles/process} - 处理字幕（识别 + 翻译）</li>
 *     <li>{@code POST /api/subtitles/session/start} - 开始录制会话</li>
 *     <li>{@code POST /api/subtitles/session/end} - 结束录制会话</li>
 * </ul>
 *
 * @author harryhelloo
 * @version 1.0
 * @since 2026-03-01
 */
@Log4j2
@RestController
@RequestMapping("/api/subtitles")
public class SubtitleController {

    /**
     * 翻译会话记录（channelId -> 是否记录）
     * <p>用于跟踪哪些频道正在录制翻译字幕</p>
     */
    private final Map<String, Boolean> translationSessions = new ConcurrentHashMap<>();
    @Autowired
    private SubtitleService subtitleService;
    @Autowired
    private TranslationService translationService;
    @Autowired
    private SettingsManager settingsManager;
    @Autowired
    private ChannelService channelService;

    /**
     * 获取字幕文件列表
     *
     * @return 字幕文件列表，包含文件名、路径、大小、最后修改时间等信息
     */
    @GetMapping("/files")
    public ResponseEntity<List<Map<String, Object>>> listSubtitles() {
        try {
            List<Map<String, Object>> files = subtitleService.getSubtitleFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("Failed to list subtitles", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取字幕文件内容
     *
     * <p>读取指定字幕文件的文本内容。</p>
     *
     * @param filename 字幕文件名
     * @return 字幕文件内容，如果文件不存在则返回 404
     */
    @GetMapping("/files/{filename}")
    public ResponseEntity<String> getSubtitle(@PathVariable String filename) {
        try {
            // 安全检查：防止路径遍历攻击
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().body("Invalid filename");
            }

            String content = subtitleService.getSubtitleContent(filename);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
        } catch (Exception e) {
            log.error("Failed to get subtitle: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 下载字幕文件
     *
     * <p>以附件形式下载指定的字幕文件。</p>
     *
     * @param filename 字幕文件名
     * @return 文件字节流，包含 Content-Disposition 头
     */
    @GetMapping("/files/{filename}/download")
    public ResponseEntity<ByteArrayResource> downloadSubtitle(@PathVariable String filename) {
        try {
            // 安全检查：防止路径遍历攻击
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            byte[] content = subtitleService.downloadSubtitle(filename);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }

            ByteArrayResource resource = new ByteArrayResource(content);

            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(content.length)
                .body(resource);
        } catch (Exception e) {
            log.error("Failed to download subtitle: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除字幕文件
     *
     * <p>删除指定的字幕文件。</p>
     *
     * @param filename 字幕文件名
     * @return 删除结果信息
     */
    @DeleteMapping("/files/{filename}/delete")
    public ResponseEntity<String> deleteSubtitle(@PathVariable String filename) {
        try {
            // 安全检查：防止路径遍历攻击
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().body("Invalid filename");
            }

            boolean deleted = subtitleService.deleteSubtitle(filename);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok("Deleted: " + filename);
        } catch (Exception e) {
            log.error("Failed to delete subtitle: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量删除字幕文件
     *
     * <p>一次性删除多个字幕文件。</p>
     *
     * @param params 请求参数，包含 filenames 列表
     * @return 删除结果，包含成功删除的文件数量
     */
    @PostMapping("/files/batch-delete")
    public ResponseEntity<String> batchDeleteSubtitles(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<String> filenames = (List<String>) params.get("filenames");

            if (filenames == null || filenames.isEmpty()) {
                return ResponseEntity.badRequest().body("No filenames provided");
            }

            int deletedCount = 0;
            for (String filename : filenames) {
                // 安全检查
                if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                    continue;
                }

                if (subtitleService.deleteSubtitle(filename)) {
                    deletedCount++;
                }
            }

            return ResponseEntity.ok("Deleted " + deletedCount + " of " + filenames.size() + " files");
        } catch (Exception e) {
            log.error("Failed to batch delete subtitles", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 按类型筛选字幕文件
     *
     * <p>根据字幕类型（source/translate）筛选字幕文件。</p>
     *
     * @param type 字幕类型（source 或 translate）
     * @return 符合条件的字幕文件列表
     */
    @GetMapping("/files/filter/{type}")
    public ResponseEntity<List<Map<String, Object>>> filterSubtitles(
        @PathVariable String type
    ) {
        try {
            List<Map<String, Object>> allFiles = subtitleService.getSubtitleFiles();
            List<Map<String, Object>> filtered = new java.util.ArrayList<>();

            for (Map<String, Object> file : allFiles) {
                String fileType = (String) file.get("type");
                if (type.equalsIgnoreCase(fileType)) {
                    filtered.add(file);
                }
            }

            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            log.error("Failed to filter subtitles", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 按频道名称筛选字幕文件
     *
     * <p>根据频道名称筛选字幕文件，支持 URL 编码的频道名称。</p>
     *
     * @param channelName 频道名称（可 URL 编码）
     * @return 符合条件的字幕文件列表
     */
    @GetMapping("/files/channel/{channelName}")
    public ResponseEntity<List<Map<String, Object>>> getSubtitlesByChannel(
        @PathVariable String channelName
    ) {
        try {
            List<Map<String, Object>> allFiles = subtitleService.getSubtitleFiles();
            List<Map<String, Object>> filtered = new java.util.ArrayList<>();

            // URL 解码频道名称
            String decodedChannelName = java.net.URLDecoder.decode(channelName, StandardCharsets.UTF_8);

            for (Map<String, Object> file : allFiles) {
                String fileChannelName = (String) file.get("channelName");
                if (decodedChannelName.equalsIgnoreCase(fileChannelName)) {
                    filtered.add(file);
                }
            }

            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            log.error("Failed to get subtitles by channel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 接收字幕数据并进行识别和翻译（统一 API）
     *
     * <p>这是字幕处理的核心 API，整合了字幕识别和翻译两个流程。</p>
     *
     * <h3>处理流程：</h3>
     * <ol>
     *     <li>验证请求参数（channelId, text, startTime, endTime）</li>
     *     <li>记录原文字幕到 SRT 文件（使用精确时间戳）</li>
     *     <li>将源字幕信息缓存（用于后续翻译同步）</li>
     *     <li>检查 Settings 中的 doTranslate 配置</li>
     *     <li>如果 doTranslate=false，返回 "no_translation"</li>
     *     <li>如果 doTranslate=true，执行流式翻译</li>
     *     <li>翻译完成后，使用缓存的源字幕时间记录翻译字幕</li>
     * </ol>
     *
     * <h3>SSE 事件类型：</h3>
     * <ul>
     *     <li>{@code source_recorded} - 原文字幕已记录</li>
     *     <li>{@code translation_chunk} - 翻译流式片段</li>
     *     <li>{@code complete} - 翻译完成</li>
     *     <li>{@code result} - 特殊结果（如 "no_translation"）</li>
     *     <li>{@code error} - 错误信息</li>
     * </ul>
     *
     * <h3>时间同步机制：</h3>
     * <p>翻译字幕使用与源字幕完全相同的时间戳，确保两种字幕在播放器中同步显示。</p>
     *
     * @param subtitleData 字幕数据对象
     *                     - channelId: 频道 ID（必填）
     *                     - text: 字幕文本（必填）
     *                     - startTime: 开始时间（毫秒，必填）
     *                     - endTime: 结束时间（毫秒，必填）
     * @return SSE Emitter，用于推送流式响应
     * @see SubtitleData
     * @see SubtitleService#writeSourceWithTime
     * @see SubtitleService#writeTranslateWithTime
     */
    @PostMapping(value = "/process", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter processSubtitle(@RequestBody SubtitleData subtitleData) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        try {
            // 1. 参数验证
            if (subtitleData == null || subtitleData.getChannelId() == null || subtitleData.getText() == null) {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\":\"Missing required fields: channelId or text\"}"));
                emitter.complete();
                return emitter;
            }

            if (subtitleData.getStartTime() == null || subtitleData.getEndTime() == null) {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\":\"Missing required fields: startTime and endTime\"}"));
                emitter.complete();
                return emitter;
            }

            if (subtitleData.getStartTime() >= subtitleData.getEndTime()) {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\":\"startTime must be less than endTime\"}"));
                emitter.complete();
                return emitter;
            }

            String channelId = subtitleData.getChannelId();
            String sourceText = subtitleData.getText();
            long startTime = subtitleData.getStartTime();
            long endTime = subtitleData.getEndTime();

            // 2. 记录原文字幕（带精确时间）
            subtitleService.writeSourceWithTime(channelId, sourceText, startTime, endTime);
            log.debug("Source subtitle recorded for channel: {}", channelId);

            // 发送原文确认事件
            emitter.send(SseEmitter.event()
                .name("source_recorded")
                .data("{\"status\":\"source_recorded\",\"text\":\"" + escapeJson(sourceText) + "\"}"));

            // 3. 检查是否启用翻译
            Boolean doTranslate = settingsManager.getSettings().getDoTranslate();
            if (doTranslate == null || !doTranslate) {
                log.debug("Translation disabled for channel: {}", channelId);
                emitter.send(SseEmitter.event()
                    .name("result")
                    .data("no_translation"));
                emitter.complete();
                return emitter;
            }

            // 4. 执行流式翻译
            String sourceLang = settingsManager.getSettings().getSourceLang();
            String targetLang = settingsManager.getSettings().getTargetLang();

            AsyncStreamResponse<String> streamResponse = translationService.translate(
                sourceText, sourceLang, targetLang);

            StringBuilder translatedText = new StringBuilder();

            streamResponse.subscribe(new AsyncStreamResponse.Handler<String>() {
                @Override
                public void onComplete(java.util.Optional<Throwable> error) {
                    if (error.isPresent()) {
                        log.error("Translation error", error.get());
                        try {
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\":\"Translation failed: " + error.get().getMessage() + "\"}"));
                            emitter.completeWithError(error.get());
                        } catch (IOException e) {
                            log.error("Failed to send error event", e);
                        }
                    } else {
                        log.debug("Translation completed for channel: {}", channelId);

                        // 5. 记录翻译字幕（使用缓存的源字幕时间）
                        subtitleService.writeTranslateWithTime(channelId, sourceText, translatedText.toString());

                        try {
                            // 发送完成事件
                            emitter.send(SseEmitter.event()
                                .name("complete")
                                .data("{\"status\":\"completed\",\"translation\":\"" + escapeJson(translatedText.toString()) + "\"}"));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("Failed to send completion event", e);
                        }
                    }
                }

                @Override
                public void onNext(String chunk) {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("translation_chunk")
                            .data(chunk));
                        translatedText.append(chunk);
                    } catch (IOException e) {
                        log.error("Failed to send translation chunk", e);
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ex) {
                            log.error("Failed to complete emitter with error", ex);
                        }
                    }
                }
            });

        } catch (Exception e) {
            log.error("Failed to process subtitle", e);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\":\"" + e.getMessage() + "\"}"));
                emitter.completeWithError(e);
            } catch (IOException ex) {
                // ignore
            }
        }

        // 处理客户端断开连接
        emitter.onTimeout(() -> {
            log.debug("SSE timeout for subtitle processing");
        });

        emitter.onError(e -> {
            log.error("SSE error for subtitle processing", e);
        });

        return emitter;
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     *
     * <p>用于将文本安全地嵌入 JSON 响应中。</p>
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * 开始字幕录制会话（用于初始化）
     *
     * <p>创建 SRT 文件写入器，准备记录字幕。</p>
     *
     * @param request 请求参数，包含 channelId
     * @return 会话启动结果
     */
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

            subtitleService.startRecording(channel);
            translationSessions.put(channelId, true);

            log.info("Subtitle session started for channel: {}", channelId);
            return ResponseEntity.ok("Session started");
        } catch (Exception e) {
            log.error("Failed to start session", e);
            return ResponseEntity.internalServerError().body("Failed to start session: " + e.getMessage());
        }
    }

    /**
     * 结束字幕录制会话
     * 
     * <p>关闭 SRT 文件写入器，清理缓存。</p>
     * 
     * @param request 请求参数，包含 channelId
     * @return 会话结束结果
     */
    @PostMapping("/session/end")
    public ResponseEntity<String> endSession(@RequestBody Map<String, String> request) {
        try {
            String channelId = request.get("channelId");

            if (channelId == null || channelId.isEmpty()) {
                return ResponseEntity.badRequest().body("channelId is required");
            }

            subtitleService.stopRecording(channelId);
            translationSessions.remove(channelId);

            log.info("Subtitle session ended for channel: {}", channelId);
            return ResponseEntity.ok("Session ended");
        } catch (Exception e) {
            log.error("Failed to end session", e);
            return ResponseEntity.internalServerError().body("Failed to end session: " + e.getMessage());
        }
    }
}
