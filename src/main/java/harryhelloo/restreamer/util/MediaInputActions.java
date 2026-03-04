package harryhelloo.restreamer.util;

/**
 * OBS WebSocket 媒体输入动作常量
 *
 * <p>定义 OBS WebSocket 协议中媒体输入源的控制动作，用于 {@code TriggerMediaInputAction} 请求。</p>
 *
 * <h2>可用动作：</h2>
 * <ul>
 *     <li>{@link #NONE} - 无操作</li>
 *     <li>{@link #PLAY} - 播放</li>
 *     <li>{@link #PAUSE} - 暂停</li>
 *     <li>{@link #STOP} - 停止</li>
 *     <li>{@link #RESTART} - 重启</li>
 *     <li>{@link #NEXT} - 下一个</li>
 *     <li>{@link #PREVIOUS} - 上一个</li>
 * </ul>
 *
 * <h2>使用示例：</h2>
 * <pre>
 * // 触发媒体源停止动作
 * obsRemoteController.triggerMediaInputAction(
 *     TriggerMediaInputActionRequest.builder()
 *         .inputName("Stream Source")
 *         .mediaAction(MediaInputActions.STOP)
 *         .build(),
 *     response -> {
 *         if (response.isSuccessful()) {
 *             log.info("Media stopped successfully");
 *         }
 *     }
 * );
 * </pre>
 *
 * @author harryhelloo
 * @version 1.0
 * @see io.obswebsocket.community.client.message.request.mediainputs.TriggerMediaInputActionRequest
 */
public class MediaInputActions {

    /**
     * 无操作
     */
    public static final String NONE = "OBS_WEBSOCKET_MEDIA_INPUT_ACTION_NONE";

    /**
     * 播放媒体源
     */
    public static final String PLAY = "OBS_WEBSOCKET_MEDIA_INPUT_ACTION_PLAY";

    /**
     * 暂停媒体源
     */
    public static final String PAUSE = "OBS_WEBSOCKET_MEDIA_INPUT_ACTION_PAUSE";

    /**
     * 停止媒体源
     */
    public static final String STOP = "OBS_WEBSOCKET_MEDIA_INPUT_ACTION_STOP";

    /**
     * 重启媒体源（先停止再播放）
     */
    public static final String RESTART = "OBS_WEBSOCKET_MEDIA_INPUT_ACTION_RESTART";

    /**
     * 下一个媒体项（播放列表）
     */
    public static final String NEXT = "OBS_WEBSOCKET_MEDIA_INPUT_ACTION_NEXT";

    /**
     * 上一个媒体项（播放列表）
     */
    public static final String PREVIOUS = "OBS_WEBSOCKET_MEDIA_INPUT_ACTION_PREVIOUS";

    /**
     * 私有构造函数，防止实例化
     */
    private MediaInputActions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
