package com.dlepe.twitchchatanalyzer.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.dlepe.twitchchatanalyzer.service.LogService;
import com.dlepe.twitchchatanalyzer.service.VideoService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.model.TwitchVod;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatAnalyticsController {

    private final LogService logService;
    private final VideoService videoService;

    @GetMapping("/chat-analytics")
    public Map<LocalDateTime, Map<String, AtomicLong>> getChatAnalyticsForChannel(
            @RequestParam final String channelName,
            @RequestParam(name = "logDate", defaultValue = "#{T(java.time.LocalDate).now()}", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate logDate) {
        final List<String> logs = logService.getLogData(channelName, logDate);
        return logService.parseChatLog(channelName, logs);
    }

    @GetMapping("/video-details/{videoId}")
    public TwitchVod getVideoDetails(
            @PathVariable final String videoId) {
        videoService.getVideo(videoId);
        return null;
    }

}
