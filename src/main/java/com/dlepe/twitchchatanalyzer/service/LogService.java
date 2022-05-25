package com.dlepe.twitchchatanalyzer.service;

import com.dlepe.twitchchatanalyzer.dto.ChatLogRecord;
import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import java.time.LocalDateTime;
import java.util.List;

public interface LogService {

    List<ChatLogRecord> getRawLogDataForVideo(final VideoDetails videoDetails);

    List<ChatLogRecord> getRawLogDataForDateRange(final String channelName,
        final LocalDateTime startTime, final LocalDateTime endTime);


    void parseChatLogs(final VideoDetails videoDetails,
        final List<ChatLogRecord> chatLogs);
}
