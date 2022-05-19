package com.dlepe.twitchchatanalyzer.service;

import java.time.LocalDateTime;
import java.util.List;

import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.ChatLogAnalysis;
import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.ChatLogRecord;

public interface LogService {
        List<ChatLogRecord> getLogDataForDateRange(final String channelName, final LocalDateTime startTime,
                        final LocalDateTime endTime);

        ChatLogAnalysis parseChatLogs(final String channelName,
                        final List<ChatLogRecord> chatLogs);
}
