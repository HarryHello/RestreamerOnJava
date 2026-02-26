package harryhelloo.restreamer.service;


import harryhelloo.restreamer.exception.ObsException;
import harryhelloo.restreamer.pojo.ObsWebsocket;
import harryhelloo.restreamer.pojo.Settings;
import io.obswebsocket.community.client.OBSRemoteController;
import io.obswebsocket.community.client.message.response.record.StartRecordResponse;
import io.obswebsocket.community.client.message.response.record.StopRecordResponse;
import io.obswebsocket.community.client.message.response.scenes.SetCurrentProgramSceneResponse;
import io.obswebsocket.community.client.message.response.stream.StartStreamResponse;
import io.obswebsocket.community.client.message.response.stream.StopStreamResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Service
public class ObsService implements Settings.ConfigurationChangeListener {
    private static final String OBS_UNREADY_MSG = "Obs service is not ready!";
    private final AtomicBoolean ready = new AtomicBoolean(false);

    private OBSRemoteController obsRemoteController;

    @EventListener(ApplicationReadyEvent.class)
    public void onAppReady() {
        // 应用启动时初始化
        initObsConnection();
        // 注册配置变更监听器
        Settings.get().addConfigurationChangeListener(this);
    }

    public void initObsConnection() {
        ObsWebsocket obsWebsocket = Settings.get().getObsWebsocket();
        if (obsWebsocket == null) {
            log.warn("OBS websocket configuration is not set");
            return;
        }

        if (ready.get() && obsRemoteController != null) {
            obsRemoteController.disconnect();
        }

        obsRemoteController = OBSRemoteController.builder()
            .host(obsWebsocket.getHost()).port(obsWebsocket.getPort()).password(obsWebsocket.getPassword())
            .autoConnect(false)
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
    }

    @Override
    public void onConfigurationChanged(String key, Object oldValue, Object newValue, Settings settings) {
        // 只监听obsWebsocket配置的变化
        if ("obsWebsocket".equals(key)) {
            log.info("OBS websocket configuration changed, reinitializing connection...");
            initObsConnection();
        }
    }

    public CompletableFuture<Boolean> connect() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        ObsWebsocket obsWebsocket = Settings.get().getObsWebsocket();
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