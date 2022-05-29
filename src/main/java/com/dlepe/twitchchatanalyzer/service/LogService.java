package com.dlepe.twitchchatanalyzer.service;

import java.time.LocalDateTime;
import java.util.Map;
import reactor.core.publisher.Flux;

public interface LogService {

    Flux<String> fetchLogsForDateRange(final String channelName,
        final LocalDateTime startTime, final LocalDateTime endTime);

    Map<String, Long> countEmotes(final String chatText);
}
