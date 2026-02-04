package harryhelloo.restreamer.controller;

import harryhelloo.restreamer.pojo.youtube.Channel;
import harryhelloo.restreamer.service.ChannelService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("api/channel")
public class ChannelStatusController {
    @Autowired
    private ChannelService ytdlpService;

    @GetMapping("/{channelId}/info")
    public Map<String, Object> getChannelInfo(@PathVariable String channelId) {
        Map<String, Object> result = new HashMap<>();
        result.put("channelId", channelId);
        try {
            // 调用 YouTube API 获取频道信息
            Channel channel = ytdlpService.getChannelName(channelId);
            String channelName = channel.getChannelName();
            result.put("channelName", channelName);
            result.put("success", true);
            log.info("Get channel name: {}", channelName);
        } catch (Exception e) {
            result.put("channelName", "未知频道");
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Failed to get channel name", e);
        }
        return result;
    }
}
