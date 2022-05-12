package com.dlepe.twitchchatanalyzer.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.dlepe.twitchchatanalyzer.service.LogService;

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

    private final WebClient webClient;
    private final static Set<String> STRINGS_TO_PARSE = Set.of("OMEGALUL", "LULW", "OMEGADANCE");
    private final static DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<String> getLogData(final String channelName) {
        Mono<String> response = webClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/channel/{channelName}/2022/5/11").build(
                        channelName))
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class);
        return Arrays.asList(response.block().split("\n"));
    }

    @Override
    public Map<LocalDateTime, Map<String, AtomicLong>> parseChatLog(final String channelName, final List<String> logs) {
        Map<LocalDateTime, Map<String, AtomicLong>> emoteCountPerMinute = new ConcurrentHashMap<>();
        logs
                .parallelStream()
                .forEach(logLine -> {
                    final Map<String, AtomicLong> emoteCountMap = new HashMap<>();
                    STRINGS_TO_PARSE
                            .stream()
                            .forEach(s -> emoteCountMap.putIfAbsent(s, new AtomicLong(0)));

                    final String[] logParts = logLine.split(" #" + channelName + " ");
                    final LocalDateTime dateTime = getTimestampToNearestMinute(logParts[0]);
                    final String chatLog = logParts[1];

                    final AtomicBoolean containsTrackedEmote = new AtomicBoolean(false);

                    // Collect counts
                    STRINGS_TO_PARSE
                            .stream()
                            .forEach(emote -> {
                                final Long occurrenceCount = kmpSearch(emote, chatLog);
                                if (occurrenceCount > 0L) {
                                    containsTrackedEmote.set(true);
                                    emoteCountMap.get(emote).addAndGet(occurrenceCount);
                                }
                            });

                    if (containsTrackedEmote.get()) {
                        emoteCountPerMinute.merge(dateTime, emoteCountMap, (v1, v2) -> {
                            v1.putAll(v2);
                            return v1;
                        });
                    }
                });

        return emoteCountPerMinute;
    }

    private LocalDateTime getTimestampToNearestMinute(final String dateStr) {
        final String timeStamp = StringUtils
                .trimTrailingCharacter(StringUtils.trimLeadingCharacter(dateStr, '['), ']');
        return LocalDateTime.parse(timeStamp, DATETIME_FORMATTER).truncatedTo(ChronoUnit.MINUTES);
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
