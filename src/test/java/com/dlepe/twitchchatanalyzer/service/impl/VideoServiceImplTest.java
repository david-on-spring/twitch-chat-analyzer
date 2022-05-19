package com.dlepe.twitchchatanalyzer.service.impl;

import java.time.Duration;

import com.dlepe.twitchchatanalyzer.service.LogService;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
public class VideoServiceImplTest {

    @Mock
    private WebClient mockWebClient;

    @Mock
    private LogService mockLogService;

    private VideoServiceImpl videoAnalysisService;

    @BeforeEach
    void setup() {
        videoAnalysisService = new VideoServiceImpl(mockWebClient, mockLogService);
    }

    @Test
    @SneakyThrows
    void testExtractHoursFromDuration() {
        final String durationText = "8h10m30s";
        final Duration duration = videoAnalysisService.getVideoDuration(durationText);

        Assert.assertEquals(8, duration.toHoursPart());
        Assert.assertEquals(10, duration.toMinutesPart());
        Assert.assertEquals(30, duration.toSecondsPart());

    }
}
