package com.dlepe.twitchchatanalyzer.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatLogAnalysis {

    private Map<LocalDateTime, Map<String, AtomicLong>> emoteMetrics;
    private LocalDateTime mostPopularOccurrence;
}
