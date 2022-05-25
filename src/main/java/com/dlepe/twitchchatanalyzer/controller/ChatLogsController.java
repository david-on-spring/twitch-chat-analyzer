package com.dlepe.twitchchatanalyzer.controller;

import com.dlepe.twitchchatanalyzer.dto.ChatLogRecord;
import com.dlepe.twitchchatanalyzer.service.LogService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat-logs")
@RequiredArgsConstructor
public class ChatLogsController {

    private final LogService logService;

    @GetMapping("/{channelName}")
    public List<ChatLogRecord> getChatLogsForChannel(
        @PathVariable final String channelName,
        @RequestParam(name = "logDate", defaultValue = "#{T(java.time.LocalDate).now()}", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate logDate) {
        final LocalDateTime startOfDay = logDate.atTime(LocalTime.MIN);
        final LocalDateTime endOfDay = logDate.atTime(LocalTime.MAX);

        // TODO: read from Redis instead of the service and cleanup service names
        return logService.getRawLogDataForDateRange(channelName,
            startOfDay,
            endOfDay);
    }
}
