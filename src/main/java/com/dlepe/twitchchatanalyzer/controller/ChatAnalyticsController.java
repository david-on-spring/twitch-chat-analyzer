package com.dlepe.twitchchatanalyzer.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.ChatLogAnalysis;
import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.ChatLogRecord;
import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.TwitchVideoAnalysis;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.dlepe.twitchchatanalyzer.service.VideoService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatAnalyticsController {

    private final LogService logService;
    private final VideoService videoService;

    @GetMapping("/chat-analytics")
    public ChatLogAnalysis getChatAnalyticsForChannel(
            @RequestParam final String channelName,
            @RequestParam(name = "logDate", defaultValue = "#{T(java.time.LocalDate).now()}", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate logDate) {
        final LocalDateTime startOfDay = logDate.atTime(LocalTime.MIN);
        final LocalDateTime endOfDay = logDate.atTime(LocalTime.MAX);

        final List<ChatLogRecord> logs = logService.getLogDataForDateRange(channelName, startOfDay, endOfDay);
        return logService.parseChatLogs(channelName, logs);
    }

    @GetMapping("/video-details/{videoId}")
    public List<TwitchVideoAnalysis> getVideoDetails(@PathVariable final String videoId) {
        return videoService.getVideo(videoId);
    }

}
