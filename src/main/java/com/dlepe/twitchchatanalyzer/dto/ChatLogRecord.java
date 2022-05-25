package com.dlepe.twitchchatanalyzer.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatLogRecord {

    private String channelName;
    private String username;
    private String chatText;
    private LocalDateTime logTimestamp;
}