package com.dlepe.twitchchatanalyzer.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public interface LogService {
    List<String> getLogData(final String channelName);

    Map<LocalDateTime, Map<String, AtomicLong>> parseChatLog(final String channelName, final List<String> logs);
}
