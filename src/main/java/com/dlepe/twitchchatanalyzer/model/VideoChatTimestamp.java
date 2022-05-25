package com.dlepe.twitchchatanalyzer.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Data
@Builder
@RedisHash("VideoChatTimestamp")
public class VideoChatTimestamp implements Serializable {

    @Id
    private String id;

    @Indexed
    private LocalDateTime timestamp;

    @Indexed
    private String videoId;

    @Indexed
    private String channelName;

    private Map<String, Long> chatMetrics;

    private String timestampUrl;
}
