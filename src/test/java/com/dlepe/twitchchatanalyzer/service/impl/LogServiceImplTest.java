package com.dlepe.twitchchatanalyzer.service.impl;

import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.ChatLogAnalysis;
import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.ChatLogRecord;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class LogServiceImplTest {

    private static final String VALID_CHAT_LOGS = "valid_chat_logs.txt";
    private static final String TEST_CHANNEL_NAME = "mizkif";

    @Mock
    private WebClient mockWebClient;

    private LogServiceImpl logService;

    @BeforeEach
    void setup() {
        logService = new LogServiceImpl(mockWebClient, testObjectMapper());
    }

    @Test
    @SneakyThrows
    void testGetLogDataForDateRange() {
        final int numberOfExpectedChatRecords = 16;
        final LocalDateTime startTime = LocalDateTime.of(2022, 05, 19, 0, 1, 0);
        final LocalDateTime endTime = LocalDateTime.of(2022, 05, 19, 23, 30, 50);

        final String logData = getTestChatLogs(VALID_CHAT_LOGS);
        setupWebClientMocks(logData);

        List<ChatLogRecord> chatLogRecords = logService.getLogDataForDateRange(TEST_CHANNEL_NAME, startTime, endTime);
        Assertions.assertEquals(numberOfExpectedChatRecords, chatLogRecords.size());
    }

    @Test
    @SneakyThrows
    void testParseChatLogs() {
        final LocalDateTime startTime = LocalDateTime.of(2022, 05, 19, 0, 0, 0);
        final LocalDateTime endTime = LocalDateTime.of(2022, 05, 19, 23, 30, 50);
        final String logData = getTestChatLogs(VALID_CHAT_LOGS);
        setupWebClientMocks(logData);

        List<ChatLogRecord> chatLogRecords = logService.getLogDataForDateRange(TEST_CHANNEL_NAME, startTime, endTime);
        ChatLogAnalysis logAnalysis = logService.parseChatLogs(TEST_CHANNEL_NAME, chatLogRecords);

        Assert.assertEquals(3, logAnalysis.emoteMetrics().keySet().size());

        // Will fix in a dedicated PR for log parsing
        // Assert.assertEquals(LocalDateTime.of(2022, 05, 19, 0, 11, 0),
        // logAnalysis.mostPopularOccurrence());
    }

    @SneakyThrows
    private String getTestChatLogs(final String filename) {
        final ClassLoader classLoader = getClass().getClassLoader();
        final String pathString = classLoader.getResource("chatlogs/" + filename).getPath();
        return Files.readString(Paths.get(pathString), StandardCharsets.UTF_8);
    }

    private void setupWebClientMocks(final String webClientResponse) {
        final var uriSpecMock = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        final var headersSpecMock = Mockito.mock(WebClient.RequestHeadersSpec.class);
        final var mediaTypeSpecMock = Mockito.mock(WebClient.RequestHeadersSpec.class);
        final var responseSpecMock = Mockito.mock(WebClient.ResponseSpec.class);

        when(mockWebClient.get()).thenReturn(uriSpecMock);
        when(uriSpecMock.uri(Mockito.any(Function.class))).thenReturn(headersSpecMock);
        when(headersSpecMock.accept(MediaType.TEXT_PLAIN)).thenReturn(mediaTypeSpecMock);
        when(mediaTypeSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(String.class))
                .thenReturn(Mono.just(webClientResponse));
    }

    private ObjectMapper testObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Don't throw an exception when json has extra fields you are
        // not serializing on. This is useful when you want to use a pojo
        // for deserialization and only care about a portion of the json
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Ignore null values when writing json.
        mapper.setSerializationInclusion(Include.NON_NULL);

        // Write times as a String instead of a Long so its human readable.
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }
}
