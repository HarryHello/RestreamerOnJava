package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.service.ObsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@Log4j2
@RestController
@RequestMapping("/api/obs")
public class ObsController {
    @Autowired
    private ObsService obsService;

    @PutMapping("/stream/start")
    public CompletableFuture<ResponseEntity<String>> startStream() {
        return obsService.startStream()
            .thenApply(response -> ResponseEntity.ok("success"))
            .exceptionally(e -> ResponseEntity.internalServerError().body("Failed to  start stream. %s".formatted(e.getMessage())));
    }

    @PutMapping("/stream/stop")
    public CompletableFuture<ResponseEntity<String>> stopStream() {
        return obsService.stopStream()
            .thenApply(response -> ResponseEntity.ok("success"))
            .exceptionally(e -> ResponseEntity.internalServerError().body("Failed to stop stream. %s".formatted(e.getMessage())));
    }

    @PutMapping("/record/start")
    public CompletableFuture<ResponseEntity<String>> startRecord() {
        return obsService.startRecord()
            .thenApply(response -> ResponseEntity.ok("success"))
            .exceptionally(e -> ResponseEntity.internalServerError().body("Failed to start record. %s".formatted(e.getMessage())));
    }

    @PutMapping("/record/stop")
    public CompletableFuture<ResponseEntity<String>> stopRecord() {
        return obsService.stopRecord()
            .thenApply(response -> ResponseEntity.ok("success"))
            .exceptionally(e -> ResponseEntity.internalServerError().body("Failed to stop record. %s".formatted(e.getMessage())));
    }

    @PutMapping("/scene/set")
    public CompletableFuture<ResponseEntity<String>> setScene(
        @RequestParam(value = "name", defaultValue = "转播") String name
    ) {
        return obsService.setScene(name)
            .thenApply(response -> ResponseEntity.ok("success"))
            .exceptionally(e -> ResponseEntity.internalServerError().body("Failed to  set scene. %s".formatted(e.getMessage())));
    }
}
