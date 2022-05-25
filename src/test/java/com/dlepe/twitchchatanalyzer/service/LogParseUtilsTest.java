package com.dlepe.twitchchatanalyzer.service;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LogParseUtilsTest {

    @Test
    void testGetTimestampToNearestMinute() {
        // Round down
        final String inputStr1 = "2022-05-19 23:04:03";
        final LocalDateTime outputDateTime1 = LogParseUtils.getTimestampToNearestMinute(inputStr1);
        Assertions.assertEquals(4L, outputDateTime1.getMinute());

        final String inputStr2 = "2022-05-19 23:04:45";
        final LocalDateTime outputDateTime2 = LogParseUtils.getTimestampToNearestMinute(inputStr2);
        Assertions.assertEquals(4L, outputDateTime2.getMinute());
    }

    @Test
    void testKmpSearch() {
        // Match
        final long countTc1 = LogParseUtils.kmpSearch("OMEGALUL", "OMEGALUL OMEGALUL OMEGALUL");
        Assertions.assertEquals(3L, countTc1);

        // No match
        final long countTc2 = LogParseUtils.kmpSearch("OMEGALUL", "some text");
        Assertions.assertEquals(0L, countTc2);

        // Empty input
        final long countTc3 = LogParseUtils.kmpSearch("OMEGALUL", null);
        Assertions.assertEquals(0L, countTc3);

        // Partial matching - in the future, should consider only matching entire tokens
        final long countTc4 = LogParseUtils.kmpSearch("LUL", "OMEGALUL");
        Assertions.assertEquals(1L, countTc4);
    }

    @Test
    @SneakyThrows
    void testExtractHoursFromDuration() {
        final String durationText = "8h10m30s";
        final Duration duration = LogParseUtils.getVideoDurationFromString(durationText);

        Assertions.assertEquals(8, duration.toHoursPart());
        Assertions.assertEquals(10, duration.toMinutesPart());
        Assertions.assertEquals(30, duration.toSecondsPart());
    }

    @Test
    void testGetVideoTimestamp() {
        final LocalDateTime startTime = LocalDateTime.now();
        final LocalDateTime endTime = startTime.plusHours(3).plusMinutes(10).plusSeconds(30);

        final String durationText = LogParseUtils.getVideoTimestampString(startTime, endTime);

        Assertions.assertEquals("3h10m30s", durationText);
    }
}
