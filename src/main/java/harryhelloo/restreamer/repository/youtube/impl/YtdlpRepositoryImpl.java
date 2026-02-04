package harryhelloo.restreamer.repository.youtube.impl;

import harryhelloo.restreamer.exception.YtdlpException;
import harryhelloo.restreamer.pojo.ProcessOutput;
import harryhelloo.restreamer.pojo.Upcoming;
import harryhelloo.restreamer.pojo.youtube.Channel;
import harryhelloo.restreamer.repository.youtube.YtdlpRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
public class YtdlpRepositoryImpl implements YtdlpRepository {
    private static final List<String> COMMANDS_FOR_UPLOADER = new ArrayList<>(List.of(
        "--no-warnings",
        "--ignore-config",
        "--skip-download",
        "--playlist-items", "1",
        "--print", "uploader"
    ));

    private static final List<String> COMMANDS_FOR_IS_LIVE = new ArrayList<>(List.of(
        "--flat-playlist",
        "--match-filter", "live_status = is_live",
        "--print", "id",
        "--print", "title",
        "--no-warnings",
        "--extractor-args", "youtubetab:skip=authcheck"
    ));
    private static final List<String> COMMANDS_FOR_IS_UPCOMING = new ArrayList<>(List.of(
        "--flat-playlist",
        "--match-filter", "live_status = is_upcoming",
        "--print", "id",
        "--print", "title",
        "--print", "release_timestamp",
        "--no-warnings"
    ));
    private static final String YTDLP_INTERRUPTED_MESSAGE = "Subprocess of Yt-flp was interrupted.";
    private static final String YTDLP_GENERAL_EXCEPTION_MESSAGE = "Cannot get channel information by Yt-dlp.";

    private static ProcessBuilder getProcessBuilder(String cookiesPath, String ytdlpPath, String url,
                                                    List<String> options) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(ytdlpPath);
        if (cookiesPath != null && !cookiesPath.isEmpty()) {
            Path path = Paths.get(cookiesPath);
            if (Files.exists(path)) {
                commandLine.add("--cookies-path");
                commandLine.add(cookiesPath);
            }
        }
        commandLine.addAll(options);
        commandLine.add(url);

        return new ProcessBuilder(commandLine).redirectErrorStream(false);
    }

    private static ProcessOutput startProcess(ProcessBuilder pb, int timeout) throws IOException, InterruptedException {
        Process process = pb.start();

        boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new YtdlpException("The subprocess of Yt-dlp timed out.");
        }

        return ProcessOutput.builder()
            .stdout(process.getInputStream())
            .stderr(process.getErrorStream())
            .exitCode(process.exitValue())
            .build();
    }

    private static void setChannelAsUnknownStatus(Channel channel) {
        channel.setCheckStream(false);
        channel.setNoStream(false);
        channel.setStreaming(false);
        channel.setUpcomingStream(false);
    }

    @Override
    public Channel getChannelName(@NotNull Channel channel, String cookiesPath, String ytdlpPath) {
        if (ytdlpPath == null) {
            ytdlpPath = "yt-dlp";
        }

        String url = MessageFormat.format("https://www.youtube.com/channel/{0}", channel.getChannelId());
        ProcessBuilder pb = getProcessBuilder(cookiesPath, ytdlpPath, url, COMMANDS_FOR_UPLOADER);

        try {
            ProcessOutput processOutput = startProcess(pb, 20);

            if (processOutput.getExitCode() != 0) {
                String errorInfo = String.join("\n", processOutput.getStderr());
                String exceptionMessage = MessageFormat.format(
                    "The subprocess of Yt-dlp exited with non-zero code: {0}, with ERROR info: {1}.",
                    processOutput.getExitCode(),
                    errorInfo
                );
                throw new YtdlpException(exceptionMessage);
            }

            if (processOutput.getStdout().isEmpty()) {
                throw new YtdlpException("The subprocess of Yt-dlp output nothing.");
            }

            String channelName = processOutput.getStdout().getFirst();
            channel.setChannelName(channelName);

            return channel;

        } catch (IOException e) {
            throw new YtdlpException(YTDLP_GENERAL_EXCEPTION_MESSAGE, e);
        } catch (InterruptedException e) {
            throw new YtdlpException(YTDLP_INTERRUPTED_MESSAGE, e);
        }


    }

    @Override
    public Channel getChannelStatus(Channel channel, String cookiesPath, String ytdlpPath) {
        setChannelAsUnknownStatus(channel);
        if (ytdlpPath == null || ytdlpPath.isEmpty()) {
            ytdlpPath = "yt-dlp";
        }

        String url = MessageFormat.format(
            "https://www.youtube.com/channel/{0}/streams",
            channel.getChannelId()
        );

        ProcessBuilder pbForIsLive = getProcessBuilder(cookiesPath, ytdlpPath, url, COMMANDS_FOR_IS_LIVE);

        try {
            ProcessOutput processOutput = startProcess(pbForIsLive, 20);

            if (processOutput.getExitCode() != 0) {
                String errorInfo = String.join("\n", processOutput.getStderr());
                if (!errorInfo.contains("No video matched") && !errorInfo.toLowerCase().contains("not found")) {
                    String exceptionMessage = MessageFormat.format(
                        "The subprocess of Yt-dlp exited with non-zero code: {0}, with error info: {1}.",
                        processOutput.getExitCode(),
                        errorInfo
                    );
                    throw new YtdlpException(exceptionMessage);
                }
            }

            if (!processOutput.getStdout().isEmpty()) {
                String streamId = processOutput.getStdout().getFirst().trim();
                String streamTitle = processOutput.getStderr().get(1).trim();
                String streamUrl = MessageFormat.format(
                    "https://www.youtube.com/watch?v={0}",
                    streamId
                );
                channel.setStreaming(true);

                channel.setStreamTitle(streamTitle);
                channel.setStreamUrl(streamUrl);

                return channel;
            }
        } catch (InterruptedException e) {
            throw new YtdlpException(YTDLP_INTERRUPTED_MESSAGE, e);
        } catch (IOException e) {
            throw new YtdlpException(YTDLP_GENERAL_EXCEPTION_MESSAGE, e);
        }

        ProcessBuilder pbForIsUpcoming = getProcessBuilder(cookiesPath, ytdlpPath, url, COMMANDS_FOR_IS_UPCOMING);

        try {
            ProcessOutput processOutput = startProcess(pbForIsUpcoming, 20);
            if (processOutput.getExitCode() != 0) {
                String errorInfo = String.join("\n", processOutput.getStderr());
                if (!errorInfo.contains("No video matched") && !errorInfo.toLowerCase().contains("not found")) {
                    String exceptionMessage = MessageFormat.format(
                        "The subprocess of Yt-dlp exited with non-zero code: {0}, with error info: {1}.",
                        processOutput.getExitCode(),
                        errorInfo
                    );
                    throw new YtdlpException(exceptionMessage);
                }
            }

            if (processOutput.getStdout().isEmpty()) {
                channel.setNoStream(true);
                return channel;
            }

            List<Upcoming> upcomings = new ArrayList<>();
            for (int i = 0; i < processOutput.getStderr().size(); i += 3) {
                if (i + 2 > processOutput.getStderr().size()) {
                    break;
                }
                upcomings.add(Upcoming.builder()
                    .streamId(processOutput.getStdout().get(i))
                    .streamTitle(processOutput.getStderr().get(i + 1))
                    .timestamp(Integer.parseInt(processOutput.getStderr().get(i + 2)))
                    .build()
                );
            }

            if (upcomings.isEmpty()) {
                channel.setNoStream(true);
                return channel;
            }

            upcomings.sort(Comparator.comparingInt(Upcoming::getTimestamp));

            String streamId = upcomings.getFirst().getStreamId();
            String streamTitle = upcomings.getFirst().getStreamTitle();
            int timestamp = upcomings.getFirst().getTimestamp();
            String streamUrl = MessageFormat.format(
                "https://www.youtube.com/watch?v={0}",
                streamId
            );
            Instant scheduledStreamTime = Instant.ofEpochSecond(timestamp);

            channel.setScheduledStreamTime(scheduledStreamTime);
            channel.setStreamUrl(streamUrl);
            channel.setStreamTitle(streamTitle);

            channel.setUpcomingStream(true);

            return channel;
        } catch (InterruptedException e) {
            throw new YtdlpException(YTDLP_INTERRUPTED_MESSAGE, e);
        } catch (IOException e) {
            throw new YtdlpException(YTDLP_GENERAL_EXCEPTION_MESSAGE, e);
        }
    }

}
