package harryhelloo.restreamer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import harryhelloo.restreamer.exception.FileException;
import harryhelloo.restreamer.pojo.HistoryStyle;
import harryhelloo.restreamer.pojo.SubtitleStyle;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Log4j2
@Service
public class StyleService {
    private static final String SUBTITLE_STYLE_FILE = "subtitle_style.json";
    private static final String HISTORY_STYLE_FILE = "history_style.json";
    private final ObjectMapper objectMapper;

    @Getter
    @Setter
    private SubtitleStyle currentSubtitleStyle = null;

    @Getter
    @Setter
    private HistoryStyle currentHistoryStyle = null;

    public StyleService(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @PostConstruct
    public void init() {
        loadSubtitleStyleFromFile();
        loadHistoryStyleFromFile();
    }

    private void loadSubtitleStyleFromFile() {
        File file = new File(SUBTITLE_STYLE_FILE);
        if (!file.exists()) {
            log.warn("{} 不存在, 创建默认样式", SUBTITLE_STYLE_FILE);
            currentSubtitleStyle = SubtitleStyle.getDefault();
            try {
                if (!file.createNewFile()) {
                    throw new FileException("Failed to create %s".formatted(SUBTITLE_STYLE_FILE));
                }
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, currentSubtitleStyle);
            } catch (IOException e) {
                throw new FileException("Failed to create %s".formatted(SUBTITLE_STYLE_FILE), e);
            }
        } else if (file.length() == 0) {
            log.warn("{} 为空, 重建默认样式", SUBTITLE_STYLE_FILE);
            currentSubtitleStyle = SubtitleStyle.getDefault();
            try {
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, currentSubtitleStyle);
            } catch (IOException e) {
                throw new FileException("Failed to reset empty %s".formatted(SUBTITLE_STYLE_FILE), e);
            }
        } else {
            try {
                currentSubtitleStyle = objectMapper.readValue(file, SubtitleStyle.class);
            } catch (IOException e) {
                throw new FileException("Failed to load from %s".formatted(SUBTITLE_STYLE_FILE), e);
            }
        }
    }

    public void saveSubtitleStyleToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(SUBTITLE_STYLE_FILE), currentSubtitleStyle);
        } catch (IOException e) {
            throw new FileException("Failed to save to %s".formatted(SUBTITLE_STYLE_FILE), e);
        }
    }

    private void loadHistoryStyleFromFile() {
        File file = new File(HISTORY_STYLE_FILE);
        if (!file.exists()) {
            log.warn("{} 不存在, 创建默认样式", HISTORY_STYLE_FILE);
            currentHistoryStyle = HistoryStyle.getDefault();
            try {
                if (!file.createNewFile()) {
                    throw new FileException("Failed to create %s".formatted(HISTORY_STYLE_FILE));
                }
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, currentHistoryStyle);
            } catch (IOException e) {
                throw new FileException("Failed to create %s".formatted(HISTORY_STYLE_FILE), e);
            }
        } else if (file.length() == 0) {
            log.warn("{} 为空, 重建默认样式", HISTORY_STYLE_FILE);
            currentHistoryStyle = HistoryStyle.getDefault();
            try {
                objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, currentHistoryStyle);
            } catch (IOException e) {
                throw new FileException("Failed to reset empty %s".formatted(HISTORY_STYLE_FILE), e);
            }
        } else {
            try {
                currentHistoryStyle = objectMapper.readValue(file, HistoryStyle.class);
            } catch (IOException e) {
                throw new FileException("Failed to load from %s".formatted(HISTORY_STYLE_FILE), e);
            }
        }
    }

    public void saveHistoryStyleToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(HISTORY_STYLE_FILE), currentHistoryStyle);
        } catch (IOException e) {
            throw new FileException("Failed to save to %s".formatted(HISTORY_STYLE_FILE), e);
        }
    }
}
