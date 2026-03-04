package harryhelloo.restreamer.service;

import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.pojo.Settings;
import harryhelloo.restreamer.pojo.StreamerChannel;
import harryhelloo.restreamer.util.SrtWriter;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 字幕服务
 * 
 * <p>负责管理直播过程中的字幕记录，包括原文字幕和翻译字幕的写入、缓存管理。</p>
 * 
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>字幕录制会话管理（开始/停止）</li>
 *     <li>原文字幕写入（支持精确时间戳）</li>
 *     <li>翻译字幕写入（与源字幕时间同步）</li>
 *     <li>源字幕缓存（用于翻译时间同步）</li>
 *     <li>字幕文件管理（列表、读取、下载、删除）</li>
 * </ul>
 * 
 * <h2>字幕缓存机制：</h2>
 * <p>当写入源字幕时，会同时缓存字幕的时间信息。翻译完成后，通过匹配原文文本
 * 查找缓存，使用相同的时间戳写入翻译字幕，确保两种字幕在播放器中同步显示。</p>
 * 
 * <h2>时间处理：</h2>
 * <p>所有时间戳都会对齐到 60fps 帧边界（约 16.67ms/帧），确保与视频帧同步。</p>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see SrtWriter
 */
@Log4j2
@Service
public class SubtitleService {

    /**
     * 当前正在进行的字幕写入器（按频道 ID 索引）
     */
    private final Map<String, SrtWriter> activeWriters = new ConcurrentHashMap<>();

    /**
     * 直播开始时间记录（按频道 ID 索引）
     */
    private final Map<String, LocalDateTime> streamStartTimes = new ConcurrentHashMap<>();

    /**
     * 源字幕缓存（按频道 ID 索引）
     * 存储已写入的源字幕信息，用于翻译时同步时间戳
     * 使用 ConcurrentLinkedQueue 保证线程安全
     */
    private final Map<String, ConcurrentLinkedQueue<SubtitleCache>> sourceSubtitleCaches = new ConcurrentHashMap<>();

    /**
     * 每个频道缓存的最大字幕数量（避免内存溢出）
     */
    private static final int MAX_CACHE_SIZE = 50;

    /**
     * 字幕缓存超时时间（毫秒）- 超过此时间的缓存会被清理
     */
    private static final long CACHE_TIMEOUT_MS = 300000; // 5 分钟

    @Autowired
    private SettingsManager settingsManager;

    /**
     * 字幕缓存数据类
     * 
     * <p>存储源字幕的精确时间信息，用于翻译时同步时间戳。</p>
     */
    @Data
    public static class SubtitleCache {
        private final String text;
        private final long startTime;
        private final long endTime;
        private final long timestamp;  // 系统时间戳，用于超时清理

        public SubtitleCache(String text, long startTime, long endTime) {
            this.text = text;
            this.startTime = startTime;
            this.endTime = endTime;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * 检查缓存是否超时
         * 
         * @return 如果缓存超时返回 true，否则返回 false
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TIMEOUT_MS;
        }
    }

    /**
     * 开始记录字幕
     *
     * <p>创建 SRT 字幕文件写入器，初始化文件目录和写入流。</p>
     * <p>注意：此方法只初始化写入器，不设置直播开始时间。直播开始时间由 {@link #setStreamStartTime(String)} 设置。</p>
     *
     * @param channel 频道信息对象，包含：
     *                - channelId: 频道唯一标识
     *                - channelName: 频道显示名称
     *                - streamUrl: 直播流地址
     */
    public void startRecording(StreamerChannel channel) {
        if (channel == null || channel.getChannelId() == null) {
            log.warn("Invalid channel provided");
            return;
        }

        String channelId = channel.getChannelId();

        if (activeWriters.containsKey(channelId)) {
            log.warn("Recording already in progress for channel: {}", channelId);
            return;
        }

        // 不设置直播开始时间，等待实际直播开始时再设置
        SrtWriter writer = new SrtWriter(LocalDateTime.now(), channel.getChannelName(), channel.getStreamUrl());
        try {
            writer.initialize();
            activeWriters.put(channelId, writer);
            log.info("Initialized subtitle recording for channel: {} ({})", channelId, channel.getChannelName());
        } catch (IOException e) {
            log.error("Failed to initialize SRT writer for channel: {}", channelId, e);
        }
    }

    /**
     * 设置直播开始时间
     * 
     * <p>当检测到直播实际开始时调用此方法，记录直播开始时间。</p>
     * <p>此时间将用于计算字幕的相对时间戳。</p>
     * 
     * @param channelId 频道 ID
     */
    public void setStreamStartTime(String channelId) {
        if (!activeWriters.containsKey(channelId)) {
            log.warn("No active writer for channel: {}, cannot set stream start time", channelId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        streamStartTimes.put(channelId, now);
        log.info("Stream start time set for channel: {} at {}", channelId, now);
    }
    
    /**
     * 停止记录字幕
     * 
     * <p>关闭 SRT 文件写入器，清理缓存和状态记录。</p>
     * 
     * @param channelId 频道 ID
     */
    public void stopRecording(String channelId) {
        SrtWriter writer = activeWriters.remove(channelId);
        if (writer != null) {
            writer.close();
            streamStartTimes.remove(channelId);
            clearSubtitleCache(channelId);
            log.info("Stopped subtitle recording for channel: {}", channelId);
        }
    }
    
    /**
     * 获取字幕文件列表
     * 
     * @return 字幕文件路径列表
     */
    public List<Map<String, Object>> getSubtitleFiles() {
        Path subtitlesDir = Paths.get("subtitles");
        List<Map<String, Object>> files = new ArrayList<>();
        
        if (!Files.exists(subtitlesDir)) {
            return files;
        }
        
        try {
            Files.list(subtitlesDir)
                .filter(path -> path.toString().endsWith(".srt"))
                .sorted(Comparator.comparingLong(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis();
                    } catch (IOException e) {
                        return 0;
                    }
                }))
                .forEach(path -> {
                    Map<String, Object> fileInfo = new java.util.HashMap<>();
                    fileInfo.put("filename", path.getFileName().toString());
                    fileInfo.put("path", path.toString());
                    fileInfo.put("size", getFileSize(path));
                    fileInfo.put("lastModified", getLastModifiedTime(path));
                    
                    // 解析文件名信息
                    String filename = path.getFileName().toString();
                    String[] parts = filename.split("_");
                    if (parts.length >= 3) {
                        fileInfo.put("timestamp", parts[0]);
                        fileInfo.put("type", parts[1]);  // source or translate
                        fileInfo.put("channelName", parts[2]);
                        if (parts.length > 3) {
                            fileInfo.put("streamLink", String.join("_", java.util.Arrays.copyOfRange(parts, 3, parts.length - 4)));
                        }
                    }
                    
                    files.add(fileInfo);
                });
        } catch (IOException e) {
            log.error("Failed to list subtitle files", e);
        }
        
        return files;
    }
    
    /**
     * 获取字幕文件内容
     * 
     * @param filename 文件名
     * @return 文件内容
     */
    public String getSubtitleContent(String filename) {
        Path path = Paths.get("subtitles", filename);
        try {
            return Files.readString(path);
        } catch (IOException e) {
            log.error("Failed to read subtitle file: {}", filename, e);
            return null;
        }
    }
    
    /**
     * 下载字幕文件
     * 
     * @param filename 文件名
     * @return 文件字节数组
     */
    public byte[] downloadSubtitle(String filename) {
        Path path = Paths.get("subtitles", filename);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("Failed to read subtitle file: {}", filename, e);
            return null;
        }
    }
    
    /**
     * 删除字幕文件
     * 
     * @param filename 文件名
     * @return 是否删除成功
     */
    public boolean deleteSubtitle(String filename) {
        Path path = Paths.get("subtitles", filename);
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Failed to delete subtitle file: {}", filename, e);
            return false;
        }
    }
    
    /**
     * 获取文件大小（字节）
     */
    private long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * 获取最后修改时间（格式化字符串）
     */
    private String getLastModifiedTime(Path path) {
        try {
            java.time.Instant instant = Files.getLastModifiedTime(path).toInstant();
            return java.time.format.DateTimeFormatter.ISO_INSTANT.format(instant);
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * 检查是否正在录制指定频道的字幕
     */
    public boolean isRecording(String channelId) {
        return activeWriters.containsKey(channelId);
    }

    /**
     * 写入带有精确时间信息的字幕（前端传入绝对时间戳）
     * 同时将源字幕信息缓存，供翻译时使用
     * 
     * <p>前端传入的是绝对时间戳（毫秒），后端会自动计算相对于直播开始时间的偏移量。</p>
     *
     * @param channelId 频道 ID
     * @param text 字幕文本
     * @param startTime 字幕开始时间（毫秒，绝对时间戳，如 System.currentTimeMillis()）
     * @param endTime 字幕结束时间（毫秒，绝对时间戳）
     * @throws IllegalStateException 当字幕写入器未初始化或直播开始时间未设置时抛出
     */
    public void writeSourceWithTime(String channelId, String text, long startTime, long endTime) {
        SrtWriter writer = activeWriters.get(channelId);
        if (writer == null) {
            log.warn("No active writer for channel: {}. Please call startRecording() first.", channelId);
            throw new IllegalStateException("Subtitle recording not initialized for channel: " + channelId);
        }

        // 获取直播开始时间
        LocalDateTime streamStartTime = streamStartTimes.get(channelId);
        if (streamStartTime == null) {
            log.warn("No stream start time found for channel: {}. Has the stream started?", channelId);
            throw new IllegalStateException("Stream start time not set for channel: " + channelId + 
                ". Waiting for stream to start...");
        }

        try {
            // 将绝对时间转换为相对于直播开始时间的偏移量
            long relativeStartTime = java.time.Duration.between(streamStartTime,
                java.time.Instant.ofEpochMilli(startTime).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()).toMillis();
            long relativeEndTime = java.time.Duration.between(streamStartTime,
                java.time.Instant.ofEpochMilli(endTime).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()).toMillis();

            // 写入源字幕
            writer.writeSourceWithTime(text, relativeStartTime, relativeEndTime);

            // 缓存源字幕信息（用于翻译同步）
            cacheSourceSubtitle(channelId, text, relativeStartTime, relativeEndTime);
            
            log.debug("Source subtitle written successfully for channel: {}, text: {}", channelId, text);
            
        } catch (Exception e) {
            log.error("Failed to write source subtitle for channel: {}", channelId, e);
            throw new RuntimeException("Failed to write source subtitle: " + e.getMessage(), e);
        }
    }

    /**
     * 写入翻译字幕（使用缓存的源字幕时间信息，保证时间同步）
     * 
     * <p>通过原文文本查找缓存的源字幕时间信息，确保翻译字幕与源字幕时间戳完全同步。</p>
     *
     * @param channelId 频道 ID
     * @param sourceText 原文文本（用于匹配缓存）
     * @param translateText 翻译文本
     * @throws IllegalStateException 当字幕写入器未初始化时抛出
     */
    public void writeTranslateWithTime(String channelId, String sourceText, String translateText) {
        SrtWriter writer = activeWriters.get(channelId);
        if (writer == null) {
            log.warn("No active writer for channel: {}", channelId);
            throw new IllegalStateException("Subtitle recording not initialized for channel: " + channelId);
        }

        // 从缓存中查找对应的源字幕
        SubtitleCache cache = findMatchingSourceSubtitle(channelId, sourceText);
        if (cache == null) {
            log.warn("No matching source subtitle found for translation: {}", sourceText);
            // 这是业务警告，但不抛出异常，因为翻译本身可能成功
            return;
        }

        try {
            // 使用与源字幕相同的时间戳
            writer.writeTranslateWithTime(translateText, cache.getStartTime(), cache.getEndTime());
            log.debug("Translate subtitle written successfully for channel: {}, text: {}", channelId, translateText);
            
        } catch (Exception e) {
            log.error("Failed to write translate subtitle for channel: {}", channelId, e);
            throw new RuntimeException("Failed to write translate subtitle: " + e.getMessage(), e);
        }
    }

    /**
     * 缓存源字幕信息
     * 
     * <p>将源字幕的时间信息存入缓存，供后续翻译时使用。</p>
     * 
     * @param channelId 频道 ID
     * @param text 字幕文本
     * @param startTime 开始时间（毫秒）
     * @param endTime 结束时间（毫秒）
     */
    private void cacheSourceSubtitle(String channelId, String text, long startTime, long endTime) {
        sourceSubtitleCaches.computeIfAbsent(channelId, k -> new ConcurrentLinkedQueue<>());
        
        ConcurrentLinkedQueue<SubtitleCache> cache = sourceSubtitleCaches.get(channelId);
        
        // 清理过期缓存
        cleanupExpiredCache(channelId);
        
        // 添加新缓存
        cache.offer(new SubtitleCache(text, startTime, endTime));
        
        // 限制缓存大小
        while (cache.size() > MAX_CACHE_SIZE) {
            cache.poll();  // 移除最旧的缓存
        }
    }

    /**
     * 查找匹配的源字幕缓存
     * 
     * <p>通过文本相似度匹配（精确匹配或包含匹配）查找对应的源字幕缓存。</p>
     * <p>找到匹配后会从缓存中移除，避免重复使用。</p>
     * 
     * @param channelId 频道 ID
     * @param sourceText 原文文本
     * @return 匹配的源字幕缓存，如果未找到返回 null
     */
    private SubtitleCache findMatchingSourceSubtitle(String channelId, String sourceText) {
        ConcurrentLinkedQueue<SubtitleCache> cache = sourceSubtitleCaches.get(channelId);
        if (cache == null || cache.isEmpty()) {
            return null;
        }

        // 清理过期缓存
        cleanupExpiredCache(channelId);

        // 查找匹配的缓存（从最新到最旧）
        List<SubtitleCache> cacheList = new ArrayList<>(cache);
        for (int i = cacheList.size() - 1; i >= 0; i--) {
            SubtitleCache entry = cacheList.get(i);
            if (entry.getText().equals(sourceText) || entry.getText().contains(sourceText)) {
                // 找到匹配，从缓存中移除（避免重复使用）
                cache.remove(entry);
                return entry;
            }
        }

        return null;
    }

    /**
     * 清理过期缓存
     * 
     * <p>移除超过 {@link #CACHE_TIMEOUT_MS} 时间的缓存条目。</p>
     * 
     * @param channelId 频道 ID
     */
    private void cleanupExpiredCache(String channelId) {
        ConcurrentLinkedQueue<SubtitleCache> cache = sourceSubtitleCaches.get(channelId);
        if (cache == null) {
            return;
        }

        cache.removeIf(SubtitleCache::isExpired);
    }

    /**
     * 清除指定频道的所有缓存
     * 
     * @param channelId 频道 ID
     */
    public void clearSubtitleCache(String channelId) {
        sourceSubtitleCaches.remove(channelId);
    }
}
