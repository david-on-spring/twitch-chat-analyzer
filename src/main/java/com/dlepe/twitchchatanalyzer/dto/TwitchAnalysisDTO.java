package com.dlepe.twitchchatanalyzer.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public sealed interface TwitchAnalysisDTO {

        record TwitchVideoAnalysis(String videoId, String videoTitle, String channelName,
                        OffsetDateTime startTime,
                        OffsetDateTime endTime, String hotspotTimestamp,
                        ChatLogAnalysis chatAnalysis)
                        implements TwitchAnalysisDTO {
        }

        record ChatLogRecord(String channelName, String username, String chatText,
                        LocalDateTime logTimestamp)
                        implements TwitchAnalysisDTO {
        }

        record ChatLogAnalysis(Map<LocalDateTime, Map<String, AtomicLong>> emoteMetrics,
                        LocalDateTime mostPopularOccurrence) {
        }

}
