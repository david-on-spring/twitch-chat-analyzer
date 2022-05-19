package com.dlepe.twitchchatanalyzer.service.impl;

import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.ChatLogRecord;

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

    @Mock
    private WebClient mockWebClient;

    private LogServiceImpl logService;

    @BeforeEach
    void setup() {
        logService = new LogServiceImpl(mockWebClient);
    }

    @Test
    @SneakyThrows
    void testGetLogDataForDateRange() {
        LocalDateTime startTime = LocalDateTime.of(2022, 05, 19, 0, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2022, 05, 19, 23, 30, 50);

        final ClassLoader classLoader = getClass().getClassLoader();
        final String pathString = classLoader.getResource("chatlogs/valid_chat_logs.txt").getPath();
        String logContent = Files.readString(Paths.get(pathString), StandardCharsets.UTF_8);

        final var uriSpecMock = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        final var headersSpecMock = Mockito.mock(WebClient.RequestHeadersSpec.class);
        final var mediaTypeSpecMock = Mockito.mock(WebClient.RequestHeadersSpec.class);
        final var responseSpecMock = Mockito.mock(WebClient.ResponseSpec.class);

        when(mockWebClient.get()).thenReturn(uriSpecMock);
        when(uriSpecMock.uri(Mockito.any(Function.class))).thenReturn(headersSpecMock);
        when(headersSpecMock.accept(MediaType.TEXT_PLAIN)).thenReturn(mediaTypeSpecMock);
        when(mediaTypeSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(String.class))
                .thenReturn(Mono.just(logContent));

        List<ChatLogRecord> chatLogRecords = logService.getLogDataForDateRange("mizkif", startTime, endTime);
        Assertions.assertNotNull(chatLogRecords);

    }

    @Test
    void testParseChatLogs() {

    }
}
