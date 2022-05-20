package com.dlepe.twitchchatanalyzer.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.ChatLogAnalysis;
import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.TwitchVideoAnalysis;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.dlepe.twitchchatanalyzer.service.VideoService;
import com.google.common.annotations.VisibleForTesting;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.swagger.model.TwitchVod;
import io.swagger.model.TwitchVodData;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

        private final static String VIDEO_TIMESTAMP_PATTERN = "([0-9]+)([hms])";

        private final WebClient twitchWebClient;
        private final LogService logService;

        @Override
        public List<TwitchVideoAnalysis> getVideo(@NonNull final String videoId) {
                final TwitchVod videoResponse = getVideoDetails(videoId);
                return videoResponse.getData()
                                .stream()
                                .map(this::analyzeVideo)
                                .collect(Collectors.toList());
        }

        private TwitchVideoAnalysis analyzeVideo(final TwitchVodData videoData) {
                // Get video metadata
                final OffsetDateTime videoStartTime = videoData.getCreatedAt()
                                .atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
                final Duration videoDuration = getVideoDuration(videoData.getDuration());
                final OffsetDateTime videoEndTime = videoStartTime
                                .plusSeconds(videoDuration.toSeconds());

                // Pull logs for the video
                final ChatLogAnalysis analysis = logService.parseChatLogs(videoData.getUserName().toLowerCase(),
                                logService.getLogDataForDateRange(videoData.getUserName().toLowerCase(),
                                                videoStartTime.toLocalDateTime(),
                                                videoEndTime.toLocalDateTime()));

                final String videoTimestamp = getVideoTimestamp(videoStartTime.toLocalDateTime(),
                                videoEndTime.toLocalDateTime());
                final String videoHotspot = videoData.getUrl() + "?t=" + videoTimestamp;

                return new TwitchVideoAnalysis(videoData.getId(), videoData.getTitle(),
                                videoData.getUserName().toLowerCase(),
                                videoStartTime, videoEndTime, videoHotspot, analysis);
        }

        private TwitchVod getVideoDetails(@NonNull final String videoId) {
                final Mono<TwitchVod> response = twitchWebClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/videos")
                                                .queryParam("id", videoId)
                                                .build(videoId))
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(TwitchVod.class);
                return response.block();
        }

        @VisibleForTesting
        protected Duration getVideoDuration(@NonNull final String videoDuration) {
                final Pattern pattern = Pattern.compile(VIDEO_TIMESTAMP_PATTERN);
                final Matcher matcher = pattern.matcher(videoDuration);

                Duration duration = Duration.ofSeconds(0);
                while (matcher.find()) {
                        final Long timeValue = Long.parseLong(matcher.group(1));
                        final String timeUnit = matcher.group(2);
                        switch (timeUnit) {
                                case "h":
                                        duration = duration.plusHours(timeValue);
                                        break;
                                case "m":
                                        duration = duration.plusMinutes(timeValue);
                                        break;
                                case "s":
                                        duration = duration.plusSeconds(timeValue);
                                        break;
                        }
                }

                return duration;
        }

        @VisibleForTesting
        protected String getVideoTimestamp(final LocalDateTime startTime, final LocalDateTime specifiedTime) {
                final Duration duration = Duration.between(startTime, specifiedTime);
                final Long totalSecondsDifference = duration.get(ChronoUnit.SECONDS);
                final Long hours = totalSecondsDifference / 3600;
                final Long minutes = (totalSecondsDifference % 3600) / 60;
                final Long seconds = totalSecondsDifference % 60;

                return String.format("%dh%dm%ds", hours, minutes, seconds);
        }

}