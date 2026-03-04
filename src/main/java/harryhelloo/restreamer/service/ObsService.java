package harryhelloo.restreamer.service;


import com.google.gson.JsonObject;
import harryhelloo.restreamer.exception.EmptyConfigException;
import harryhelloo.restreamer.exception.ObsException;
import harryhelloo.restreamer.manager.SettingsManager;
import harryhelloo.restreamer.pojo.ObsWebsocket;
import harryhelloo.restreamer.pojo.Settings;
import io.obswebsocket.community.client.OBSRemoteController;
import io.obswebsocket.community.client.message.request.general.GetVersionRequest;
import io.obswebsocket.community.client.message.request.inputs.GetInputSettingsRequest;
import io.obswebsocket.community.client.message.request.inputs.SetInputSettingsRequest;
import io.obswebsocket.community.client.message.request.mediainputs.GetMediaInputStatusRequest;
import io.obswebsocket.community.client.message.request.mediainputs.TriggerMediaInputActionRequest;
import io.obswebsocket.community.client.message.request.sceneitems.GetSceneItemListRequest;
import io.obswebsocket.community.client.message.request.sceneitems.SetSceneItemEnabledRequest;
import io.obswebsocket.community.client.message.request.scenes.GetCurrentProgramSceneRequest;
import io.obswebsocket.community.client.message.response.general.GetVersionResponse;
import io.obswebsocket.community.client.message.response.inputs.GetInputSettingsResponse;
import io.obswebsocket.community.client.message.response.inputs.SetInputSettingsResponse;
import io.obswebsocket.community.client.message.response.mediainputs.GetMediaInputStatusResponse;
import io.obswebsocket.community.client.message.response.mediainputs.TriggerMediaInputActionResponse;
import io.obswebsocket.community.client.message.response.record.StartRecordResponse;
import io.obswebsocket.community.client.message.response.record.StopRecordResponse;
import harryhelloo.restreamer.util.MediaInputActions;
import io.obswebsocket.community.client.message.response.sceneitems.GetSceneItemListResponse;
import io.obswebsocket.community.client.message.response.scenes.GetCurrentProgramSceneResponse;
import io.obswebsocket.community.client.message.response.scenes.SetCurrentProgramSceneResponse;
import io.obswebsocket.community.client.message.response.stream.StartStreamResponse;
import io.obswebsocket.community.client.message.response.stream.StopStreamResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OBS WebSocket 服务
 * 
 * <p>提供与 OBS Studio 的 WebSocket 通信功能，用于控制直播推流、录制、场景切换等操作。</p>
 * 
 * <h2>主要功能：</h2>
 * <ul>
 *     <li>OBS 连接管理（初始化、重连）</li>
 *     <li>直播控制（开始/停止推流）</li>
 *     <li>录制控制（开始/停止录制）</li>
 *     <li>场景管理（切换场景）</li>
 *     <li>媒体源控制（重启源、设置流链接）</li>
 *     <li>媒体状态监控</li>
 * </ul>
 * 
 * <h2>OBS WebSocket 配置：</h2>
 * <pre>
 * {
 *   "obsWebsocket": {
 *     "host": "localhost",
 *     "port": 4455,
 *     "password": "your_password"
 *   }
 * }
 * </pre>
 * 
 * <h2>使用示例：</h2>
 * <pre>
 * // 开始推流
 * obsService.startStream().thenAccept(response -> {
 *     log.info("Stream started");
 * });
 * 
 * // 设置流链接
 * obsService.setStreamLinkSourceUrl("Stream Source", "https://example.com/stream.m3u8");
 * </pre>
 * 
 * @author harryhelloo
 * @version 1.0
 * @see io.obswebsocket.community.client.OBSRemoteController
 */
@Log4j2
@Service
public class ObsService implements SettingsManager.ConfigurationChangeListener {
    
    /**
     * OBS 未就绪时的错误消息
     */
    private static final String OBS_UNREADY_MSG = "Obs service is not ready!";
    
    /**
     * OBS 连接就绪状态标志
     */
    private final AtomicBoolean ready = new AtomicBoolean(false);

    /**
     * OBS WebSocket 远程控制器
     */
    private OBSRemoteController obsRemoteController;

    /**
     * 应用启动时初始化 OBS 连接
     * 
     * <p>如果配置不存在，则跳过 OBS 初始化（OBS 功能不可用）。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onAppReady() {
        // 应用启动时初始化
        initObsConnection();
        // 注册配置变更监听器
        SettingsManager.getInstance().addConfigurationChangeListener(this);
    }

    /**
     * 初始化 OBS WebSocket 连接
     * 
     * <p>从 Settings 获取配置并建立 WebSocket 连接。</p>
     * <p>如果配置不存在，则记录警告并跳过初始化（OBS 功能不可用）。</p>
     */
    public void initObsConnection() {
        ObsWebsocket obsWebsocket = SettingsManager.getInstance().getSettings().getObsWebsocket();
        if (obsWebsocket == null) {
            log.warn("OBS websocket configuration is not set. OBS features will be unavailable.");
            ready.set(false);
            return;
        }

        String host = obsWebsocket.getHost();
        int port = obsWebsocket.getPort();
        String password = obsWebsocket.getPassword();

        if (host == null || host.trim().isEmpty()) {
            log.warn("OBS host is not set. OBS features will be unavailable.");
            ready.set(false);
            return;
        }

        if (port <= 0 || port > 65535) {
            log.warn("OBS port is not correctly set. OBS features will be unavailable.");
            ready.set(false);
            return;
        }

        if (password.trim().isEmpty()) {
            password = null;
        }

        if (ready.get() && obsRemoteController != null) {
            obsRemoteController.disconnect();
        }

        try {
            obsRemoteController = OBSRemoteController.builder()
                .host(host).port(port).password(password)
                .autoConnect(true)  // 改为自动连接
                .lifecycle()
                .onReady(() -> {
                    ready.set(true);
                    log.info("OBS connect successfully!");
                })
                .onDisconnect(() -> {
                    ready.set(false);
                    log.warn("OBS disconnect.");
                })
                .onCommunicatorError(e -> {
                    log.error("OBS communicator error!", e.getThrowable());
                    ready.set(false);
                })
                .onControllerError(e -> {
                    log.error("OBS controller error!", e.getThrowable());
                    ready.set(false);
                })
                .and()
                .build();
        } catch (Exception e) {
            log.error("Failed to connect to OBS: {}", e.getMessage());
            ready.set(false);
        }
    }

    /**
     * 配置变更回调
     * 
     * <p>监听 OBS WebSocket 配置变更，自动重新连接。</p>
     * 
     * @param key 变更的键
     * @param oldValue 旧值
     * @param newValue 新值
     * @param settings 完整的配置对象
     */
    @Override
    public void onConfigurationChanged(String key, Object oldValue, Object newValue, Settings settings) {
        // 只监听 obsWebsocket 配置的变化
        if ("obsWebsocket".equals(key)) {
            log.info("OBS websocket configuration changed, reinitializing connection...");
            initObsConnection();
        }
    }

    /**
     * 检查 OBS 是否已连接并就绪
     *
     * @return true 如果 OBS 已就绪
     */
    public boolean isReady() {
        return ready.get();
    }

    /**
     * 获取 OBS 版本信息
     *
     * @return 版本信息的 CompletableFuture
     */
    public CompletableFuture<GetVersionResponse> getVersion() {
        CompletableFuture<GetVersionResponse> future = new CompletableFuture<>();

        if (!ready.get()) {
            future.completeExceptionally(new IllegalStateException(OBS_UNREADY_MSG));
            return future;
        }

        obsRemoteController.sendRequest(
            GetVersionRequest.builder().build(),
            (GetVersionResponse response) -> {
                if (response.isSuccessful()) {
                    future.complete(response);
                } else {
                    future.completeExceptionally(new ObsException("Failed to get version: " +
                        response.getMessageData().getRequestStatus().getComment()));
                }
            }
        );

        return future;
    }

    public CompletableFuture<Boolean> connect() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        ObsWebsocket obsWebsocket = SettingsManager.getInstance().getSettings().getObsWebsocket();
        if (obsWebsocket == null || obsRemoteController == null) {
            future.completeExceptionally(new ObsException("OBS has not been initialized!"));
            return future;
        }

        obsRemoteController.connect();
        future.complete(ready.get());
        return future;
    }

    public CompletableFuture<SetCurrentProgramSceneResponse> setScene(String sceneName) {
        CompletableFuture<SetCurrentProgramSceneResponse> future = new CompletableFuture<>();

        if (!ready.get()) {
            future.completeExceptionally(new IllegalStateException(OBS_UNREADY_MSG));
            return future;
        }

        obsRemoteController.setCurrentProgramScene(sceneName, response -> {
            if (response.isSuccessful()) {
                log.info("OBS set scene: {} successfully!", sceneName);
                future.complete(response);
            } else {
                future.completeExceptionally(new ObsException("Failed to set scene: %s".formatted(sceneName)));
            }
        });

        return future;
    }

    /**
     * 设置 Stream Link Source 的直播链接
     * 适用于 Chimildic 的 obs-vlc-video-plugin 插件添加的 Streamlink Video Source
     * 设置后会自动重启源（通过切换可见性）以使新链接生效
     *
     * @param inputName Stream Link Source 的名称（在 OBS 中显示的名称）
     * @param streamUrl 直播流链接
     * @return 设置结果的 CompletableFuture
     */
    public CompletableFuture<SetInputSettingsResponse> setStreamLinkSourceUrl(String inputName, String streamUrl) {
        CompletableFuture<SetInputSettingsResponse> future = new CompletableFuture<>();

        if (!ready.get()) {
            future.completeExceptionally(new IllegalStateException(OBS_UNREADY_MSG));
            return future;
        }

        // 先获取当前的输入设置
        obsRemoteController.sendRequest(
            GetInputSettingsRequest.builder()
                .inputName(inputName)
                .build(),
            (GetInputSettingsResponse response) -> {
                if (!response.isSuccessful()) {
                    future.completeExceptionally(new ObsException("Failed to get input settings: " +
                        response.getMessageData().getRequestStatus().getComment()));
                    return;
                }

                try {
                    // 获取当前的 inputSettings
                    JsonObject settings = response.getInputSettings();
                    String inputKind = response.getInputKind();

                    log.info("Input kind: {}, current settings: {}", inputKind, settings);

                    // 设置 streamlink_url 参数
                    // 这是 obs-vlc-video-plugin 插件中 Stream Link Source 的配置参数
                    settings.addProperty("streamlink_url", streamUrl);

                    // 设置播放列表
                    obsRemoteController.sendRequest(
                        SetInputSettingsRequest.builder()
                            .inputName(inputName)
                            .inputSettings(settings)
                            .overlay(false) // false = 完全替换现有设置
                            .build(),
                        (SetInputSettingsResponse setResponse) -> {
                            if (setResponse.isSuccessful()) {
                                log.info("Stream Link Source URL set successfully for input: {}, URL: {}", inputName, streamUrl);
                                // 设置成功后重启源
                                restartSource(inputName)
                                    .thenAccept(v -> future.complete(setResponse))
                                    .exceptionally(ex -> {
                                        log.warn("Failed to restart source: {}, but URL was set successfully", ex.getMessage());
                                        future.complete(setResponse);
                                        return null;
                                    });
                            } else {
                                future.completeExceptionally(new ObsException("Failed to set Stream Link Source URL: " +
                                    setResponse.getMessageData().getRequestStatus().getComment()));
                            }
                        }
                    );
                } catch (Exception e) {
                    future.completeExceptionally(new ObsException("Error setting Stream Link Source URL: " + e.getMessage(), e));
                }
            }
        );

        return future;
    }

    /**
     * 重启源（通过切换可见性 + 媒体控制）
     * 先获取当前场景，然后找到源并切换可见性，最后触发媒体停止和重启
     *
     * @param sourceName 源名称
     * @return CompletableFuture
     */
    private CompletableFuture<Void> restartSource(String sourceName) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        // 获取当前节目场景
        obsRemoteController.sendRequest(
            GetCurrentProgramSceneRequest.builder()
                .build(),
            (GetCurrentProgramSceneResponse sceneResponse) -> {
                if (!sceneResponse.isSuccessful()) {
                    log.warn("Failed to get current scene: {}", sceneResponse.getMessageData().getRequestStatus().getComment());
                    future.complete(null);
                    return;
                }

                String currentSceneName = sceneResponse.getCurrentProgramSceneName();
                log.debug("Current scene: {}", currentSceneName);

                // 获取场景中的源列表
                obsRemoteController.sendRequest(
                    GetSceneItemListRequest.builder()
                        .sceneName(currentSceneName)
                        .build(),
                    (GetSceneItemListResponse sceneListResponse) -> {
                        if (!sceneListResponse.isSuccessful()) {
                            log.warn("Failed to get scene items: {}", sceneListResponse.getMessageData().getRequestStatus().getComment());
                            future.complete(null);
                            return;
                        }

                        // 查找匹配的源
                        Integer sceneItemId = null;
                        for (var sceneItem : sceneListResponse.getSceneItems()) {
                            if (sourceName.equals(sceneItem.getSourceName())) {
                                sceneItemId = sceneItem.getSceneItemId();
                                break;
                            }
                        }

                        if (sceneItemId == null) {
                            log.warn("Source '{}' not found in scene '{}'", sourceName, currentSceneName);
                            future.complete(null);
                            return;
                        }

                        // 切换可见性并触发媒体控制
                        toggleSourceVisibilityAndMedia(currentSceneName, sourceName, sceneItemId)
                            .thenAccept(v -> future.complete(null))
                            .exceptionally(ex -> {
                                log.warn("Error restarting source: {}", ex.getMessage());
                                future.complete(null);
                                return null;
                            });
                    }
                );
            }
        );

        return future;
    }

    /**
     * 切换源的可见性并触发媒体控制（停止 + 重启）
     *
     * @param sceneName   场景名称
     * @param sourceName  源名称
     * @param sceneItemId 场景项 ID
     * @return CompletableFuture
     */
    private CompletableFuture<Void> toggleSourceVisibilityAndMedia(String sceneName, String sourceName, Integer sceneItemId) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        log.info("Restarting source '{}' by toggling visibility and media control...", sourceName);

        // 1. 关闭源
        obsRemoteController.sendRequest(
            SetSceneItemEnabledRequest.builder()
                .sceneName(sceneName)
                .sceneItemId(sceneItemId)
                .sceneItemEnabled(false)
                .build(),
            (disableResponse) -> {
                if (!disableResponse.isSuccessful()) {
                    log.warn("Failed to disable source: {}", disableResponse.getMessageData().getRequestStatus().getComment());
                } else {
                    log.debug("Source '{}' disabled", sourceName);
                }

                // 短暂延迟
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 2. 打开源
                obsRemoteController.sendRequest(
                    SetSceneItemEnabledRequest.builder()
                        .sceneName(sceneName)
                        .sceneItemId(sceneItemId)
                        .sceneItemEnabled(true)
                        .build(),
                    (enableResponse) -> {
                        if (!enableResponse.isSuccessful()) {
                            log.warn("Failed to enable source: {}", enableResponse.getMessageData().getRequestStatus().getComment());
                        } else {
                            log.debug("Source '{}' enabled", sourceName);
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        // 3. 触发媒体 STOP
                        triggerMediaAction(sourceName, MediaInputActions.STOP)
                            .thenCompose(v -> {
                                // 延迟后触发 RESTART
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                return triggerMediaAction(sourceName, MediaInputActions.RESTART);
                            })
                            .thenAccept(v -> {
                                log.info("Source '{}' restarted successfully (visibility + media control)", sourceName);
                                future.complete(null);
                            })
                            .exceptionally(ex -> {
                                log.warn("Media control completed with warnings: {}", ex.getMessage());
                                future.complete(null);
                                return null;
                            });
                    }
                );
            }
        );

        return future;
    }

    /**
     * 触发媒体输入动作
     * 使用 OBS WebSocket 协议的 TriggerMediaInputAction 请求
     *
     * @param inputName 输入源名称
     * @param action    媒体动作，使用 MediaInputActions 常量
     * @return CompletableFuture
     */
    public CompletableFuture<Void> triggerMediaAction(String inputName, String action) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        log.info("Sending media action '{}' for input '{}'", action, inputName);

        obsRemoteController.sendRequest(
            TriggerMediaInputActionRequest.builder()
                .inputName(inputName)
                .mediaAction(action)
                .build(),
            (TriggerMediaInputActionResponse response) -> {
                if (response.isSuccessful()) {
                    log.info("Media action '{}' successfully triggered for input '{}'", action, inputName);
                    future.complete(null);
                } else {
                    String errorMsg = response.getMessageData().getRequestStatus().getComment();
                    log.warn("Failed to trigger media action '{}': {}", action, errorMsg);
                    future.complete(null);  // 不失败，只是警告
                }
            }
        );

        return future;
    }

    /**
     * 获取媒体输入源的状态
     *
     * @param inputName 输入源名称
     * @return CompletableFuture，包含媒体状态
     */
    public CompletableFuture<GetMediaInputStatusResponse> getMediaInputStatus(String inputName) {
        CompletableFuture<GetMediaInputStatusResponse> future = new CompletableFuture<>();

        if (!ready.get()) {
            future.completeExceptionally(new IllegalStateException(OBS_UNREADY_MSG));
            return future;
        }

        obsRemoteController.sendRequest(
            GetMediaInputStatusRequest.builder()
                .inputName(inputName)
                .build(),
            (GetMediaInputStatusResponse response) -> {
                if (response.isSuccessful()) {
                    log.debug("Media status for '{}': {}", inputName, response.getMediaState());
                    future.complete(response);
                } else {
                    String errorMsg = response.getMessageData().getRequestStatus().getComment();
                    log.warn("Failed to get media status for '{}': {}", inputName, errorMsg);
                    future.complete(response);
                }
            }
        );

        return future;
    }

    public CompletableFuture<StartStreamResponse> startStream() {
        CompletableFuture<StartStreamResponse> future = new CompletableFuture<>();

        if (!ready.get()) {
            future.completeExceptionally(new IllegalStateException(OBS_UNREADY_MSG));
            return future;
        }

        obsRemoteController.startStream(response -> {
            if (response.isSuccessful()) {
                log.info("OBS start stream successfully!");
                future.complete(response);
            } else {
                future.completeExceptionally(new ObsException("Failed to start stream!"));
            }
        });

        return future;
    }

    public CompletableFuture<StopStreamResponse> stopStream() {
        CompletableFuture<StopStreamResponse> future = new CompletableFuture<>();

        if (!ready.get()) {
            future.completeExceptionally(new IllegalStateException(OBS_UNREADY_MSG));
            return future;
        }

        obsRemoteController.stopStream(response -> {
            if (response.isSuccessful()) {
                log.info("OBS stop stream successfully!");
                future.complete(response);
            } else {
                future.completeExceptionally(new ObsException("Failed to stop stream!"));
            }
        });

        return future;
    }

    public CompletableFuture<StartRecordResponse> startRecord() {
        CompletableFuture<StartRecordResponse> future = new CompletableFuture<>();

        if (!ready.get()) {
            future.completeExceptionally(new IllegalStateException(OBS_UNREADY_MSG));
            return future;
        }

        obsRemoteController.startRecord(response -> {
            if (response.isSuccessful()) {
                log.info("OBS start record successfully!");
                future.complete(response);
            } else {
                future.completeExceptionally(new ObsException("Failed to start record!"));
            }
        });

        return future;
    }

    public CompletableFuture<StopRecordResponse> stopRecord() {
        CompletableFuture<StopRecordResponse> future = new CompletableFuture<>();

        if (!ready.get()) {
            future.completeExceptionally(new IllegalStateException(OBS_UNREADY_MSG));
            return future;
        }

        obsRemoteController.stopRecord(response -> {
            if (response.isSuccessful()) {
                log.info("OBS stop record successfully!");
                future.complete(response);
            } else {
                future.completeExceptionally(new ObsException("Failed to stop record!"));
            }
        });

        return future;
    }
}
