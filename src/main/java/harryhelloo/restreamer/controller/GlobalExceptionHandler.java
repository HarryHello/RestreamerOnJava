package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.exception.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * <p>统一处理应用程序中的各种异常，返回标准化的错误响应。</p>
 *
 * <h2>处理的异常类型：</h2>
 * <h3>客户端错误（400 Bad Request）：</h3>
 * <ul>
 *     <li>{@link EmptyConfigException} - 配置缺失</li>
 *     <li>{@link IllegalArgumentException} - 非法参数</li>
 *     <li>{@link IllegalStateException} - 非法状态（如未初始化就使用）</li>
 *     <li>{@link UrlException} - URL 错误</li>
 * </ul>
 *
 * <h3>服务端错误（5xx）：</h3>
 * <ul>
 *     <li>{@link NetworkException} - 网络错误（503 Service Unavailable）</li>
 *     <li>{@link YtdlpException} - yt-dlp 错误（500 Internal Server Error）</li>
 *     <li>{@link FileException} - 文件操作错误（500 Internal Server Error）</li>
 *     <li>{@link ObsException} - OBS 错误（500 Internal Server Error）</li>
 *     <li>其他 Exception - 通用错误（500 Internal Server Error）</li>
 * </ul>
 *
 * <h2>错误响应格式：</h2>
 * <pre>
 * {
 *   "message": "错误描述信息",
 *   "timestamp": 1234567890
 * }
 * </pre>
 *
 * @author harryhelloo
 * @version 1.0
 * @see ErrorResponse
 */
@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理配置缺失异常
     * 
     * @param e EmptyConfigException
     * @return 错误响应（400 Bad Request）
     */
    @ExceptionHandler(EmptyConfigException.class)
    public ResponseEntity<ErrorResponse> handleEmptyConfigException(@NotNull EmptyConfigException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理非法参数异常
     *
     * @param e IllegalArgumentException
     * @return 错误响应（400 Bad Request）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(@NotNull IllegalArgumentException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理非法状态异常
     *
     * @param e IllegalStateException
     * @return 错误响应（400 Bad Request）
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(@NotNull IllegalStateException e) {
        log.warn("Illegal state: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理 URL 异常
     *
     * @param e UrlException
     * @return 错误响应（400 Bad Request）
     */
    @ExceptionHandler(UrlException.class)
    public ResponseEntity<ErrorResponse> handleUrlException(@NotNull UrlException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理网络异常
     * 
     * @param e NetworkException
     * @return 错误响应（503 Service Unavailable）
     */
    @ExceptionHandler(NetworkException.class)
    public ResponseEntity<ErrorResponse> handleNetworkException(@NotNull NetworkException e) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    /**
     * 处理 yt-dlp 异常
     * 
     * @param e YtdlpException
     * @return 错误响应（500 Internal Server Error）
     */
    @ExceptionHandler(YtdlpException.class)
    public ResponseEntity<ErrorResponse> handleYtdlpException(@NotNull YtdlpException e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * 处理文件异常
     * 
     * @param e FileException
     * @return 错误响应（500 Internal Server Error）
     */
    @ExceptionHandler(FileException.class)
    public ResponseEntity<ErrorResponse> handleFileException(@NotNull FileException e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * 处理 OBS 异常
     * 
     * @param e ObsException
     * @return 错误响应（500 Internal Server Error）
     */
    @ExceptionHandler(ObsException.class)
    public ResponseEntity<ErrorResponse> handleObsException(@NotNull ObsException e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * 处理通用异常
     * 
     * @param e Exception
     * @return 错误响应（500 Internal Server Error）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(@NotNull Exception e) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * 构建错误响应
     * 
     * @param status HTTP 状态码
     * @param message 错误消息
     * @return 错误响应实体
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(@NotNull HttpStatus status, @Nullable String message) {
        ErrorResponse response = ErrorResponse.builder().message(message).timestamp(System.currentTimeMillis()).build();
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 错误响应数据对象
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ErrorResponse {
        private String message;
        private Number timestamp;
    }
}
