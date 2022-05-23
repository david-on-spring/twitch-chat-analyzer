package com.dlepe.twitchchatanalyzer.repository.operations;

import com.dlepe.twitchchatanalyzer.model.VideoChatTimestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

@AllArgsConstructor(staticName = "of")
public class VideoTimestampInsertUpdateOperation implements SessionCallback<List<Object>> {

    public static final String VIDEO_CHAT_TIMESTAMP = "VideoChatTimestamp";
    private final VideoChatTimestamp incomingRecord;

    @Override
    public <K, V> List<Object> execute(RedisOperations<K, V> redisOperations)
        throws DataAccessException {
        var operations = (RedisTemplate<Object, Object>) redisOperations;
        var hashOperations = operations.opsForHash();

        var existingTimestampRecord = (VideoChatTimestamp) hashOperations.get(VIDEO_CHAT_TIMESTAMP,
            incomingRecord.getTimestamp());
        if (Objects.nonNull(existingTimestampRecord)) {
            // If a record exists, merge to the existing counts
            try {
                operations.multi();
                final Map<String, Long> existingMetrics = existingTimestampRecord.getChatMetrics();
                incomingRecord.getChatMetrics()
                    .forEach((k, v) -> existingMetrics.merge(k, v, (v1, v2) -> v1 + v2));
                existingTimestampRecord.setChatMetrics(existingMetrics);
                hashOperations.put(VIDEO_CHAT_TIMESTAMP, existingTimestampRecord.getTimestamp(),
                    existingTimestampRecord);
                return operations.exec();
            } catch (Exception e) {
                operations.discard();
            }
        } else {
            // If a record doesn't already exist, simply persist a new one
            try {
                hashOperations.put(VIDEO_CHAT_TIMESTAMP, incomingRecord.getTimestamp(),
                    incomingRecord);
                return operations.exec();
            } catch (Exception e) {
                operations.discard();
            }
        }
        return Collections.emptyList();
    }
}
