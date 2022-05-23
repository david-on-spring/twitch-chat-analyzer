package com.dlepe.twitchchatanalyzer.service.impl;

import com.dlepe.twitchchatanalyzer.config.TwitchEmoteConfiguration;
import com.dlepe.twitchchatanalyzer.dto.ChatLogRecord;
import com.dlepe.twitchchatanalyzer.model.VideoChatTimestamp;
import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import com.dlepe.twitchchatanalyzer.repository.operations.VideoTimestampInsertUpdateOperation;
import com.dlepe.twitchchatanalyzer.service.LogParseUtils;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.google.common.annotations.VisibleForTesting;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    @VisibleForTesting
    protected final static String LOGS_API_PATH = "/channel/{channelName}/{logYear}/{logMonth}/{logDay}/";

    private final WebClient logsWebClient;
    private final TwitchEmoteConfiguration configuredEmotes;
    private final RedisTemplate<Object, Object> redisTemplate;


    @Override
    public List<ChatLogRecord> getRawLogDataForDateRange(final String channelName,
        final LocalDateTime startTime, final LocalDateTime endTime) {
        return startTime.toLocalDate()
            .datesUntil(endTime.toLocalDate().plusDays(1))
            .flatMap(day -> getRawLogData(channelName, day,
                startTime, endTime).stream())
            .collect(Collectors.toList());
    }

    private List<ChatLogRecord> getRawLogData(final String channelName, final LocalDate logsDate,
        final LocalDateTime startTime,
        final LocalDateTime endTime) {
        log.debug(String.format(
            "Fetching log data for [channelName=%s] [logDate=%s] [startTime=%s] [endTime=%s]",
            channelName, logsDate, startTime, endTime));
        final Mono<String> response = logsWebClient
            .get()
            .uri(uriBuilder -> uriBuilder.path(LOGS_API_PATH).build(
                channelName.toLowerCase(),
                logsDate.getYear(),
                logsDate.getMonthValue(),
                logsDate.getDayOfMonth()))
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .bodyToMono(String.class);

        // Optimize by grouping aggregating all chat text having similar timestamps
        // Also, consider skipping any chat text where chatText.length < smallest monitored emote
        return Arrays.asList(response.block().split("\n")).stream()
            .map(logLine -> buildChatLogRecord(logLine, channelName))
            .filter((logRecord) -> Objects.nonNull(logRecord)
                && (logRecord.getLogTimestamp().isAfter(startTime)
                || logRecord.getLogTimestamp().isEqual(startTime))
                && (logRecord.getLogTimestamp().isBefore(endTime)
                || logRecord.getLogTimestamp().isEqual(endTime)))
            .collect(Collectors.toList());
    }

    private ChatLogRecord buildChatLogRecord(final String logLine, final String channelName) {
        try {
            final String[] logParts = logLine.split(" #" + channelName + " ");
            final LocalDateTime logTimestamp = LogParseUtils.getTimestampToNearestMinute(
                logParts[0]);
            final String[] chatLogParts = logParts[1].split(":", 2);
            final String chatterUsername = chatLogParts[0];
            final String chatText = chatLogParts[1].strip();
            return ChatLogRecord.builder()
                .channelName(channelName)
                .username(chatterUsername)
                .chatText(chatText)
                .logTimestamp(logTimestamp)
                .build();
        } catch (Exception e) {
            log.error("Ignoring log line due to an issue parsing the log line", e);
            return null;
        }
    }

    @VisibleForTesting
    protected Map<String, Long> parseChatMessage(final String chatText) {
        final Map<String, Long> emoteCountMap = new HashMap<>();

        configuredEmotes.getKeywords().keySet().forEach(emotion -> {
            emoteCountMap.compute(emotion, (key, value) -> {
                // Get the list of emotes mapped to the emotion
                final List<String> emotes = configuredEmotes.getKeywords().get(emotion);

                // Assign an emotion score by totaling the count of each occurrence of emote
                return emotes.stream().map(emote -> LogParseUtils.kmpSearch(emote, chatText))
                    .reduce(0L, (subtotal, element) -> subtotal + element);
            });
        });

        return emoteCountMap;
    }

    @Override
    public List<ChatLogRecord> getRawLogDataForVideo(VideoDetails videoDetails) {
        return getRawLogDataForDateRange(videoDetails.getChannelName(),
            videoDetails.getVideoStartTime(), videoDetails.getVideoEndTime());
    }

    @Override
    @SneakyThrows
    public void parseChatLogs(final VideoDetails videoDetails,
        final List<ChatLogRecord> chatLogs) {
        chatLogs
            .forEach(logRecord -> {
                final Map<String, Long> metrics = parseChatMessage(logRecord.getChatText());

                // Remove any non-zero values
                metrics.values().removeIf(v -> v < 1L);

                // If all metrics were zero, skip the record
                if (metrics.values().stream().noneMatch(v -> v > 0L)) {
                    return;
                }

                final VideoChatTimestamp videoChatTimestamp = VideoChatTimestamp.builder()
                    .videoId(videoDetails.getId())
                    .timestamp(logRecord.getLogTimestamp())
                    .chatMetrics(metrics)
                    .build();

                // Persist to Redis and merge into existing results, if any are available
                redisTemplate.execute(
                    VideoTimestampInsertUpdateOperation.of(videoChatTimestamp));
            });
    }
}
