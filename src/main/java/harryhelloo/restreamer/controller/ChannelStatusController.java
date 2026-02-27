package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.pojo.youtube.Channel;
import harryhelloo.restreamer.service.ChannelService;
import harryhelloo.restreamer.service.ChannelStatusMonitor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Log4j2
@RestController
@RequestMapping("/api/channel")
public class ChannelStatusController {
    @Autowired
    private ChannelService channelService;
    @Autowired
    private ChannelStatusMonitor channelStatusMonitor;

    @GetMapping("/{channelId}/name")
    public ResponseEntity<String> getChannelName(@PathVariable String channelId) {
        try {
            Channel channel = channelService.getChannelName(channelId);
            String channelName = channel.getChannelName();
            log.info("Get channel name: {}", channelName);
            return ResponseEntity.ok(channelName);
        } catch (Exception e) {
            log.error("Failed to get channel name", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{channelId}/monitor/start")
    public ResponseEntity<String> startMonitoring(@PathVariable String channelId) {
        try {
            channelStatusMonitor.startMonitoring(channelId);
            return ResponseEntity.ok("Monitoring started");
        } catch (Exception e) {
            log.error("Failed to start monitoring", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{channelId}/monitor/stop")
    public ResponseEntity<String> stopMonitoring(@PathVariable String channelId) {
        try {
            channelStatusMonitor.stopMonitoring(channelId);
            return ResponseEntity.ok("Monitoring stopped");
        } catch (Exception e) {
            log.error("Failed to stop monitoring", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{channelId}/monitor/status")
    public ResponseEntity<Channel> getCurrentStatus(@PathVariable String channelId) {
        try {
            Channel channel = channelStatusMonitor.getCurrentStatus(channelId);
            return channel != null ? ResponseEntity.ok(channel) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get current status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{channelId}/monitor/sse")
    public SseEmitter streamStatus(@PathVariable String channelId) {
        return channelStatusMonitor.registerSseEmitter(channelId);
    }
}