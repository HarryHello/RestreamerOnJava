package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.pojo.youtube.Channel;
import harryhelloo.restreamer.service.ChannelService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/api/channel")
public class ChannelStatusController {
    @Autowired
    private ChannelService channelService;

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
}
