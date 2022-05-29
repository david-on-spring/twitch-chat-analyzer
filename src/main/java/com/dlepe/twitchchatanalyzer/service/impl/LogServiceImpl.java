package com.dlepe.twitchchatanalyzer.service.impl;

import com.dlepe.twitchchatanalyzer.config.TwitchEmoteConfiguration;
import com.dlepe.twitchchatanalyzer.service.LogParseUtils;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.google.common.annotations.VisibleForTesting;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    @VisibleForTesting
    protected final static String LOGS_API_PATH =
        "/channel/{channelName" + "}/{logYear}/{logMonth}/{logDay}/";

    private final WebClient logsWebClient;
    private final TwitchEmoteConfiguration configuredEmotes;

    @Override
    public Flux<String> fetchLogsForDateRange(final String channelName,
        final LocalDateTime startTime, final LocalDateTime endTime) {
        return Flux.merge(startTime.toLocalDate()
            .datesUntil(endTime.toLocalDate()
                .plusDays(1))
            .map(day -> fetchLogsForDate(channelName, day, startTime, endTime))
            .collect(Collectors.toList()));
    }

    @VisibleForTesting
    public Map<String, Long> countEmotes(final String chatText) {
        final Map<String, Long> emoteCountMap = new HashMap<>();

        configuredEmotes.getKeywords()
            .keySet()
            .forEach(emotion -> {
                emoteCountMap.compute(emotion, (key, value) -> {
                    // Get the list of emotes mapped to the emotion
                    final List<String> emotes = configuredEmotes.getKeywords()
                        .get(emotion);

                    // Assign an emotion score by totaling the count of
                    // each occurrence of emote
                    return emotes.stream()
                        .map(emote -> LogParseUtils.kmpSearch(emote, chatText))
                        .reduce(0L, Long::sum);
                });
            });

        return emoteCountMap;
    }

    private Mono<String> fetchLogsForDate(final String channelName, final LocalDate logsDate,
        final LocalDateTime startTime, final LocalDateTime endTime) {
        log.info(String.format("Fetching log data for [channelName=%s] "
                + "[logDate=%s] [startTime=%s] [endTime=%s]", channelName, logsDate, startTime,
            endTime));
        return logsWebClient.get()
            .uri(uriBuilder -> uriBuilder.path(LOGS_API_PATH)
                .build(channelName.toLowerCase(), logsDate.getYear(), logsDate.getMonthValue(),
                    logsDate.getDayOfMonth()))
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .bodyToMono(String.class);
    }
}
