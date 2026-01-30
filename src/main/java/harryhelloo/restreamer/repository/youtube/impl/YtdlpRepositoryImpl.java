package harryhelloo.restreamer.repository.youtube.impl;

import harryhelloo.restreamer.exception.YtdlpException;
import harryhelloo.restreamer.pojo.youtube.Channel;
import harryhelloo.restreamer.repository.youtube.YtdlpRepository;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class YtdlpRepositoryImpl implements YtdlpRepository {

    @Override
    public Channel getChannelInfo(@NotNull Channel channel, @NotNull String cookiesPath, String ytdlpPath) {
        if (ytdlpPath == null) {
            ytdlpPath = "yt-dlp";
        }

        String url = MessageFormat.format("https://www.youtube.com/channel/{0}", channel.getChannelId());
        List<String> commandLine = new  ArrayList<>(List.of (
            ytdlpPath,
            "--cookies", cookiesPath,
            "--no-warnings",
            "--ignore-config",
            "--skip-download",
            "--playlist-items", "1",
            "--print", "uploader",
            url
        ));

        ProcessBuilder pb = new ProcessBuilder(commandLine);
        pb.redirectErrorStream(false);
        try {
            Process process = pb.start();

            boolean finished = process.waitFor(20, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new YtdlpException("Subprocess for Yt-dlp timed out");
            }

            String stdout = new StringBuffer();
            String stderrBuffer = new StringBuffer();

            ExecutorService executor = Executors.newFixedThreadPool(2)
            Future<?> stdoutFuture = executor.submit(() -> {read})

        } catch (IOException e) {
            throw new YtdlpException("Cannot get channel information by Yt-dlp", e);
        } catch (InterruptedException e) {
            throw new YtdlpException("Subprocess for Yt-flp was interrupted", e);
        }


    }
}
