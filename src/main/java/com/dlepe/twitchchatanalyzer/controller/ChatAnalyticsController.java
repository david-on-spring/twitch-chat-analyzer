package com.dlepe.twitchchatanalyzer.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.dlepe.twitchchatanalyzer.service.LogService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatAnalyticsController {

    private final LogService logService;

    @GetMapping("/chat-analytics")
    public Map<LocalDateTime, Map<String, AtomicLong>> getChatAnalyticsForChannel(
            @RequestParam final String channelName) {
        final List<String> logs = logService.getLogData(channelName);
        return logService.parseChatLog(channelName, logs);
    }

}
