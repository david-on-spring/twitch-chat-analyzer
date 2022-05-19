package com.dlepe.twitchchatanalyzer.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.ChatLogAnalysis;
import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.ChatLogRecord;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.google.common.annotations.VisibleForTesting;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    // TODO: Drive this from a configuration
    private final static Set<String> STRINGS_TO_PARSE = Set.of("OMEGALUL", "LULW", "OMEGADANCE");
    private final static DateTimeFormatter MESSAGE_TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss");
    @VisibleForTesting
    protected final static String LOGS_API_PATH = "/channel/{channelName}/{logYear}/{logMonth}/{logDay}/";

    private final WebClient logsWebClient;
    private final Comparator<LocalDateTime> dateComparator = (o1, o2) -> o1.compareTo(o2);

    @Override
    public List<ChatLogRecord> getLogDataForDateRange(final String channelName, final LocalDateTime startTime,
            final LocalDateTime endTime) {
        return startTime.toLocalDate().datesUntil(endTime.toLocalDate().plusDays(1))
                .flatMap(day -> {
                    return getLogData(channelName, day, startTime, endTime).stream();
                }).collect(Collectors.toList());
    }

    private List<ChatLogRecord> getLogData(final String channelName, final LocalDate logsDate,
            final LocalDateTime startTime,
            final LocalDateTime endTime) {
        log.info(String.format("Fetching log data for [channelName=%s] [logDate=%s] [startTime=%s] [endTime=%s]",
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

        return Arrays.asList(response.block().split("\n")).stream()
                .map(logLine -> buildChatLogRecord(logLine, channelName))
                .filter((logRecord) -> Objects.nonNull(logRecord) && logRecord.logTimestamp().isAfter(startTime)
                        && logRecord.logTimestamp().isBefore(endTime))
                .collect(Collectors.toList());
    }

    private ChatLogRecord buildChatLogRecord(final String logLine, final String channelName) {
        try {
            log.debug(logLine);
            final String[] logParts = logLine.split(" #" + channelName + " ");
            final LocalDateTime logTimestamp = getTimestampToNearestMinute(logParts[0]);
            final String[] chatLogParts = logParts[1].split(":", 2);
            final String chatterUsername = chatLogParts[0];
            final String chatText = chatLogParts[1].strip();
            return new ChatLogRecord(channelName, chatterUsername, chatText, logTimestamp);
        } catch (Exception e) {
            log.error("Ignoring log line due to an issue parsing the log line");
            return null;
        }

    }

    @Override
    public ChatLogAnalysis parseChatLogs(final String channelName,
            final List<ChatLogRecord> chatLogs) {
        Map<LocalDateTime, Map<String, AtomicLong>> emoteCountPerMinute = new ConcurrentSkipListMap<LocalDateTime, Map<String, AtomicLong>>(
                dateComparator);

        AtomicLong mostPopularOccurrence = new AtomicLong(0L);
        AtomicReference<LocalDateTime> mostPopularTimestamp = new AtomicReference<>();
        chatLogs
                .parallelStream()
                .forEach(logRecord -> {
                    final Map<String, AtomicLong> emoteCountMap = new HashMap<>();
                    final AtomicBoolean containsTrackedEmote = new AtomicBoolean(false);

                    // Initialize counts
                    STRINGS_TO_PARSE
                            .stream()
                            .forEach(s -> emoteCountMap.putIfAbsent(s, new AtomicLong(0)));

                    final LocalDateTime logTimestampRounded = logRecord.logTimestamp().truncatedTo(ChronoUnit.MINUTES);
                    // Collect counts
                    STRINGS_TO_PARSE
                            .stream()
                            .forEach(emote -> {
                                final Long occurrenceCount = kmpSearch(emote, logRecord.chatText());
                                if (occurrenceCount > 0L) {
                                    containsTrackedEmote.set(true);
                                    emoteCountMap.get(emote).addAndGet(occurrenceCount);

                                    // Logging for the most popular moment
                                    if (occurrenceCount > mostPopularOccurrence.get()) {
                                        mostPopularOccurrence.set(occurrenceCount);
                                        mostPopularTimestamp.set(logTimestampRounded);
                                    }
                                }
                            });

                    if (containsTrackedEmote.get()) {
                        emoteCountPerMinute.merge(logTimestampRounded, emoteCountMap, (v1, v2) -> {
                            v1.putAll(v2);
                            return v1;
                        });
                    }
                });
        // TODO: compute the timestamp value to the funniest moment on stream!
        return new ChatLogAnalysis(emoteCountPerMinute, mostPopularTimestamp.get());
    }

    private LocalDateTime getTimestampToNearestMinute(final String dateStr) {
        final String timeStamp = StringUtils
                .trimTrailingCharacter(StringUtils.trimLeadingCharacter(dateStr, '['), ']');
        return LocalDateTime.parse(timeStamp, MESSAGE_TIMESTAMP_FORMATTER).truncatedTo(ChronoUnit.MINUTES);
    }

    private Long kmpSearch(String pattern, String text) {
        Long count = 0L;

        // base case 1: pattern is null or empty
        if (pattern == null || pattern.length() == 0) {
            return count;
        }

        // base case 2: text is NULL, or text's length is less than that of pattern's
        if (text == null || pattern.length() > text.length()) {
            System.out.println("Pattern not found");
            return count;
        }

        char[] chars = pattern.toCharArray();

        // next[i] stores the index of the next best partial match
        int[] next = new int[pattern.length() + 1];
        for (int i = 1; i < pattern.length(); i++) {
            int j = next[i + 1];

            while (j > 0 && chars[j] != chars[i]) {
                j = next[j];
            }

            if (j > 0 || chars[j] == chars[i]) {
                next[i + 1] = j + 1;
            }
        }

        for (int i = 0, j = 0; i < text.length(); i++) {
            if (j < pattern.length() && text.charAt(i) == pattern.charAt(j)) {
                if (++j == pattern.length()) {
                    count++;
                }
            } else if (j > 0) {
                j = next[j];
                i--; // since `i` will be incremented in the next iteration
            }
        }

        return count;
    }
}
