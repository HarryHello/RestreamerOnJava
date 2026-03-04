package harryhelloo.restreamer.util;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SRT 字幕文件工具类
 *
 * <p>负责创建和写入 SRT 格式的字幕文件，支持原文字幕和翻译字幕的同步写入。</p>
 *
 * <h2>SRT 文件格式：</h2>
 * <pre>
 * 1
 * 00:00:01,000 --> 00:00:04,000
 * Hello World
 *
 * 2
 * 00:00:04,500 --> 00:00:07,500
 * This is a subtitle
 * </pre>
 *
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>创建 SRT 字幕文件（原文和翻译）</li>
 *     <li>写入带精确时间戳的字幕</li>
 *     <li>时间对齐到 60fps 帧边界</li>
 *     <li>自动管理字幕序号和间隔</li>
 *     <li>防止字幕时间重叠</li>
 * </ul>
 *
 * <h2>文件命名规则：</h2>
 * <p>{@code [yyyy-MM-dd-HH-mm-ss]_source_channelName_streamLink.srt}</p>
 * <p>{@code [yyyy-MM-dd-HH-mm-ss]_translate_channelName_streamLink.srt}</p>
 *
 * @author harryhelloo
 * @version 1.0
 */
@Log4j2
public class SrtWriter {

    /**
     * 文件名时间格式：[yyyy-MM-dd-HH-mm-ss]
     * <p>注意：时分秒使用连字符（避免 Windows 文件名非法字符），整个时间用中括号包裹</p>
     */
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("'['yyyy-MM-dd-HH-mm-ss']'");

    /**
     * SRT 时间戳格式：HH:mm:ss,SSS
     */
    private static final DateTimeFormatter SRT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss,SSS");

    /**
     * 帧率（60 帧/秒）
     * <p>用于将时间戳对齐到帧边界</p>
     */
    private static final double FPS = 60.0;

    /**
     * 每个字符的估计帧数（用于估算开始时间）
     * <p>每个字符约 2 帧（60fps 下约 0.033 秒/字符）</p>
     */
    private static final double FRAMES_PER_CHAR = 2.0;

    /**
     * -- GETTER --
     * 获取字幕文件目录路径
     *
     * @return 字幕文件目录路径
     */
    @Getter
    private final Path subtitlesDir;
    private final LocalDateTime startTime;
    private final String channelName;
    private final String streamLink;
    private BufferedWriter sourceWriter;
    private BufferedWriter translateWriter;
    private int sourceIndex = 1;
    private int translateIndex = 1;
    private long lastSourceEndTime = 0;  // 上一条字幕的结束时间（毫秒）
    private long lastTranslateEndTime = 0;
    private boolean isClosed = false;

    /**
     * 创建 SRT 写入器
     *
     * @param startTime   直播开始时间
     * @param channelName 频道名称
     * @param streamLink  直播链接
     */
    public SrtWriter(LocalDateTime startTime, String channelName, String streamLink) {
        this.startTime = startTime;
        this.channelName = sanitizeFilename(channelName);
        this.streamLink = sanitizeFilename(streamLink);
        this.subtitlesDir = Paths.get("subtitles");

        // 确保目录存在
        try {
            Files.createDirectories(subtitlesDir);
        } catch (IOException e) {
            log.error("Failed to create subtitles directory", e);
        }
    }

    /**
     * 初始化文件写入器
     *
     * <p>创建原文和翻译字幕文件，初始化 BufferedWriter。</p>
     *
     * @throws IOException 文件创建失败时抛出
     */
    public void initialize() throws IOException {
        if (sourceWriter != null) {
            return;  // 已初始化
        }

        String timestamp = startTime.format(FILENAME_FORMATTER);
        String sourceFilename = String.format("%s_source_%s_%s.srt", timestamp, channelName, streamLink);
        String translateFilename = String.format("%s_translate_%s_%s.srt", timestamp, channelName, streamLink);

        Path sourcePath = subtitlesDir.resolve(sourceFilename);
        Path translatePath = subtitlesDir.resolve(translateFilename);

        sourceWriter = Files.newBufferedWriter(sourcePath);
        translateWriter = Files.newBufferedWriter(translatePath);

        log.info("SRT files created: {} and {}", sourcePath, translatePath);
    }

    /**
     * 写入原文字幕
     *
     * @param text         字幕文本
     * @param receivedTime 接收到字幕的时间（相对于直播开始时间的毫秒数）
     * @deprecated 已废弃，建议使用 {@link #writeSourceWithTime(String, long, long)}
     */
    @Deprecated(since = "2026-03-01", forRemoval = true)
    public synchronized void writeSource(String text, long receivedTime) {
        if (isClosed || sourceWriter == null) {
            return;
        }

        try {
            // 估算持续时间（基于文本长度）
            long duration = estimateDuration(text);

            // 计算开始时间：max(估计值，上条的结束时间)
            long estimatedStartTime = receivedTime - duration;
            long startTime = Math.max(estimatedStartTime, lastSourceEndTime);
            long endTime = startTime + duration;

            // 写入 SRT
            writeSrtEntry(sourceWriter, sourceIndex++, startTime, endTime, text);

            lastSourceEndTime = endTime;
        } catch (IOException e) {
            log.error("Failed to write source subtitle", e);
        }
    }

    /**
     * 写入带有精确时间信息的原文字幕（前端识别的字幕开始和结束时间）
     *
     * <p>将时间戳对齐到 60fps 帧边界，并确保不与前一条字幕重叠。</p>
     *
     * @param text      字幕文本
     * @param startTime 字幕开始时间（毫秒，相对于直播开始时间）
     * @param endTime   字幕结束时间（毫秒，相对于直播开始时间）
     */
    public synchronized void writeSourceWithTime(String text, long startTime, long endTime) {
        if (isClosed || sourceWriter == null) {
            return;
        }

        try {
            // 将 endTime 对齐到帧边界（保持精确的结束时刻）
            long alignedEndTime = alignToFrame(endTime);

            // 计算 idealStartTime，但如果与前一条重叠，只调整开始时间
            long idealStartTime = alignToFrame(startTime);
            long actualStartTime = Math.max(idealStartTime, lastSourceEndTime);

            // 写入 SRT（endTime 保持不变，startTime 可能被推迟）
            writeSrtEntry(sourceWriter, sourceIndex++, actualStartTime, alignedEndTime, text);

            lastSourceEndTime = alignedEndTime;
        } catch (IOException e) {
            log.error("Failed to write source subtitle with precise time", e);
        }
    }

    /**
     * 写入翻译字幕
     *
     * @param text               翻译后的文本
     * @param receivedTime       接收到字幕的时间（相对于直播开始时间的毫秒数）
     * @param sourceReceivedTime 对应原文的接收时间（用于同步时间戳）
     * @deprecated 已废弃，建议使用 {@link #writeTranslateWithTime(String, long, long)}
     */
    @Deprecated(since = "2026-03-01", forRemoval = true)
    public synchronized void writeTranslate(String text, long receivedTime, long sourceReceivedTime) {
        if (isClosed || translateWriter == null) {
            return;
        }

        try {
            // 估算持续时间（基于文本长度）
            long duration = estimateDuration(text);

            // 计算开始时间：使用与原文相同的时间基准
            long estimatedStartTime = sourceReceivedTime - estimateDuration(text);
            long startTime = Math.max(estimatedStartTime, lastTranslateEndTime);
            long endTime = startTime + duration;

            // 写入 SRT
            writeSrtEntry(translateWriter, translateIndex++, startTime, endTime, text);

            lastTranslateEndTime = endTime;
        } catch (IOException e) {
            log.error("Failed to write translate subtitle", e);
        }
    }

    /**
     * 写入带有精确时间信息的翻译字幕（使用与源字幕相同的时间戳）
     *
     * <p>将时间戳对齐到 60fps 帧边界，并确保不与前一条翻译字幕重叠。</p>
     *
     * @param text      翻译文本
     * @param startTime 字幕开始时间（毫秒，相对于直播开始时间）- 与源字幕同步
     * @param endTime   字幕结束时间（毫秒，相对于直播开始时间）- 与源字幕同步
     */
    public synchronized void writeTranslateWithTime(String text, long startTime, long endTime) {
        if (isClosed || translateWriter == null) {
            return;
        }

        try {
            // 对齐到帧边界
            long alignedStartTime = alignToFrame(startTime);
            long alignedEndTime = alignToFrame(endTime);

            // 确保不与前一条字幕重叠
            long actualStartTime = Math.max(alignedStartTime, lastTranslateEndTime);

            // 写入 SRT
            writeSrtEntry(translateWriter, translateIndex++, actualStartTime, alignedEndTime, text);

            lastTranslateEndTime = alignedEndTime;
        } catch (IOException e) {
            log.error("Failed to write translate subtitle with precise time", e);
        }
    }

    /**
     * 写入原文和翻译字幕（保持时间戳同步）
     *
     * <p>同时写入原文和翻译字幕，使用相同的开始和结束时间。</p>
     *
     * @param sourceText    原文文本
     * @param translateText 翻译文本
     * @param receivedTime  接收到字幕的时间（相对于直播开始时间的毫秒数）
     * @deprecated 已废弃，建议使用 {@link #writeSourceWithTime} 和 {@link #writeTranslateWithTime}
     */
    @Deprecated(since = "2026-03-01", forRemoval = true)
    public synchronized void writeBoth(String sourceText, String translateText, long receivedTime) {
        if (isClosed || sourceWriter == null || translateWriter == null) {
            return;
        }

        try {
            // 分别估算持续时间
            long sourceDuration = estimateDuration(sourceText);
            long translateDuration = estimateDuration(translateText);

            // 使用较长的持续时间
            long duration = Math.max(sourceDuration, translateDuration);

            // 计算开始时间
            long estimatedStartTime = receivedTime - duration;
            long sourceStartTime = Math.max(estimatedStartTime, lastSourceEndTime);
            long translateStartTime = Math.max(estimatedStartTime, lastTranslateEndTime);

            // 使用相同的开始和结束时间（确保同步）
            long startTime = Math.max(sourceStartTime, translateStartTime);
            long endTime = startTime + duration;

            // 写入两条字幕
            writeSrtEntry(sourceWriter, sourceIndex++, startTime, endTime, sourceText);
            writeSrtEntry(translateWriter, translateIndex++, startTime, endTime, translateText);

            lastSourceEndTime = endTime;
            lastTranslateEndTime = endTime;
        } catch (IOException e) {
            log.error("Failed to write both subtitles", e);
        }
    }

    /**
     * 估算字幕持续时间（毫秒）
     *
     * <p>基于文本长度和帧率计算字幕的显示持续时间。</p>
     *
     * <h3>计算规则：</h3>
     * <ul>
     *     <li>帧数 = 字符数 × FRAMES_PER_CHAR</li>
     *     <li>最小持续时间：2 秒（120 帧）</li>
     *     <li>最大持续时间：8 秒（480 帧）</li>
     * </ul>
     *
     * @param text 字幕文本
     * @return 持续时间（毫秒）
     */
    private long estimateDuration(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 基于字符数估算帧数
        double frames = text.length() * FRAMES_PER_CHAR;

        // 最小持续时间：2 秒（120 帧）
        // 最大持续时间：8 秒（480 帧）
        double minFrames = FPS * 2;
        double maxFrames = FPS * 8;

        frames = Math.max(minFrames, Math.min(maxFrames, frames));

        // 转换为毫秒
        return (long) (frames / FPS * 1000);
    }

    /**
     * 将时间对齐到 60fps 帧边界
     *
     * <p>将任意时间戳向下取整到最近的帧边界，确保时间戳与视频帧同步。</p>
     *
     * <h3>计算公式：</h3>
     * <pre>
     * frameDuration = 1000 / 60 ≈ 16.67ms
     * alignedTime = (milliseconds / frameDuration) * frameDuration
     * </pre>
     *
     * @param milliseconds 时间（毫秒）
     * @return 对齐到帧边界的时间（毫秒）
     */
    private long alignToFrame(long milliseconds) {
        long frameDuration = (long) (1000 / FPS);  // ≈16.67ms
        return (milliseconds / frameDuration) * frameDuration;
    }

    /**
     * 写入单条 SRT 记录
     *
     * <p>向字幕文件写入一条完整的字幕记录，包括序号、时间戳和文本。</p>
     *
     * @param writer    写入器
     * @param index     字幕序号（从 1 开始）
     * @param startTime 开始时间（毫秒）
     * @param endTime   结束时间（毫秒）
     * @param text      字幕文本
     * @throws IOException 写入失败时抛出
     */
    private void writeSrtEntry(BufferedWriter writer, int index, long startTime, long endTime, String text) throws IOException {
        writer.write(String.valueOf(index));
        writer.newLine();
        writer.write(formatTime(startTime) + " --> " + formatTime(endTime));
        writer.newLine();
        writer.write(text);
        writer.newLine();
        writer.newLine();  // 空行分隔
        writer.flush();
    }

    /**
     * 格式化时间为 SRT 格式 (HH:mm:ss,SSS)
     *
     * <p>将毫秒时间戳转换为 SRT 标准格式：时：分：秒，毫秒</p>
     *
     * @param milliseconds 时间（毫秒）
     * @return SRT 格式的时间字符串
     */
    private String formatTime(long milliseconds) {
        long hours = milliseconds / 3600000;
        long minutes = (milliseconds % 3600000) / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        long millis = milliseconds % 1000;

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }

    /**
     * 清理文件名，移除非法字符
     *
     * <p>替换文件名中的非法字符（如 Windows 不允许的字符）为下划线。</p>
     *
     * @param filename 原始文件名
     * @return 清理后的文件名
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        // 移除或替换文件名中的非法字符
        // 注意：保留 .srt 扩展名的点，但替换 URL 中的点
        return filename.replaceAll("[<>:\"/\\\\|？*]", "_")
            .replaceAll("\\.", "_")  // 替换点为下划线
            .replaceAll("\\?", "_")  // 替换问号为下划线
            .replaceAll("&", "_")    // 替换&为下划线
            .replaceAll("=", "_")    // 替换=为下划线
            .replaceAll("\\s+", "_")
            .trim();
    }

    /**
     * 关闭写入器
     *
     * <p>关闭所有文件写入器，释放资源。</p>
     */
    public synchronized void close() {
        if (isClosed) {
            return;
        }

        try {
            if (sourceWriter != null) {
                sourceWriter.close();
            }
            if (translateWriter != null) {
                translateWriter.close();
            }
            log.info("SRT files closed");
        } catch (IOException e) {
            log.error("Failed to close SRT files", e);
        } finally {
            isClosed = true;
        }
    }

    /**
     * 获取原文文件名
     *
     * @return 原文字幕文件名
     */
    public String getSourceFilename() {
        String timestamp = startTime.format(FILENAME_FORMATTER);
        return String.format("%s_source_%s_%s.srt", timestamp, channelName, streamLink);
    }

    /**
     * 获取翻译文件名
     *
     * @return 翻译字幕文件名
     */
    public String getTranslateFilename() {
        String timestamp = startTime.format(FILENAME_FORMATTER);
        return String.format("%s_translate_%s_%s.srt", timestamp, channelName, streamLink);
    }
}
