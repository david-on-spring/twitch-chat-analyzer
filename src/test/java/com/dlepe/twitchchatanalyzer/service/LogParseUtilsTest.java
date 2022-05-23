package com.dlepe.twitchchatanalyzer.service;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class LogParseUtilsTest {

    @Test
    void testGetTimestampToNearestMinute() {
        // Round down
        final String inputStr1 = "2022-05-19 23:04:03";
        final LocalDateTime outputDateTime1 = LogParseUtils.getTimestampToNearestMinute(inputStr1);
        Assert.assertEquals(4L, outputDateTime1.getMinute());

        final String inputStr2 = "2022-05-19 23:04:45";
        final LocalDateTime outputDateTime2 = LogParseUtils.getTimestampToNearestMinute(inputStr2);
        Assert.assertEquals(4L, outputDateTime2.getMinute());
    }

    @Test
    void testKmpSearch() {
        // Match
        final long countTc1 = LogParseUtils.kmpSearch("OMEGALUL", "OMEGALUL OMEGALUL OMEGALUL");
        Assert.assertEquals(3L, countTc1);

        // No match
        final long countTc2 = LogParseUtils.kmpSearch("OMEGALUL", "some text");
        Assert.assertEquals(0L, countTc2);

        // Empty input
        final long countTc3 = LogParseUtils.kmpSearch("OMEGALUL", null);
        Assert.assertEquals(0L, countTc3);

        // Partial matching - in the future, should consider only matching entire tokens
        final long countTc4 = LogParseUtils.kmpSearch("LUL", "OMEGALUL");
        Assert.assertEquals(1L, countTc4);
    }

    @Test
    @SneakyThrows
    void testExtractHoursFromDuration() {
        final String durationText = "8h10m30s";
        final Duration duration = LogParseUtils.getVideoDurationFromString(durationText);

        Assert.assertEquals(8, duration.toHoursPart());
        Assert.assertEquals(10, duration.toMinutesPart());
        Assert.assertEquals(30, duration.toSecondsPart());

    }
}
