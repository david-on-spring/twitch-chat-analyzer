package com.dlepe.twitchchatanalyzer.service.impl;

import com.dlepe.twitchchatanalyzer.exception.NoSuchElementException;
import com.dlepe.twitchchatanalyzer.model.VideoChatTimestamp;
import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import com.dlepe.twitchchatanalyzer.model.mapper.TwitchVideoMapper;
import com.dlepe.twitchchatanalyzer.repository.VideoChatTimestampRepository;
import com.dlepe.twitchchatanalyzer.repository.VideoDetailsRepository;
import com.dlepe.twitchchatanalyzer.service.LogParseUtils;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.dlepe.twitchchatanalyzer.service.TwitchHelixService;
import com.dlepe.twitchchatanalyzer.service.VideoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import io.swagger.model.TwitchHelixUserResponse;
import io.swagger.model.TwitchHelixUserResponseData;
import io.swagger.model.TwitchHelixVideoResponse;
import io.swagger.model.TwitchHelixVideoResponseData;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final LogService logService;
    private final TwitchHelixService twitchHelixService;
    private final VideoDetailsRepository videoRepository;
    private final VideoChatTimestampRepository videoChatTimestampRepository;
    private final TwitchVideoMapper twitchVideoMapper;
    private final ObjectMapper objectMapper;

    @Override
    public VideoDetails getVideoByVideoId(@NonNull final String videoId) {
        VideoDetails result;

        var videoDetails = videoRepository.findById(videoId);
        if (videoDetails.isEmpty()) {
            final TwitchHelixVideoResponse videoResponse = twitchHelixService.getVideoDetailsForVideoId(
                videoId);
            final Optional<TwitchHelixVideoResponseData> vodData = videoResponse.getData().stream()
                .filter(v -> videoId.equals(v.getId())).findFirst();
            if (vodData.isEmpty()) {
                throw new NoSuchElementException("Cannot find a video for the specified ID");
            }
            videoDetails = Optional.of(twitchVideoMapper.toVideoDetails(vodData.get()));
            result = videoRepository.save(videoDetails.get());
        } else {
            result = videoDetails.get();
        }
        return result;
    }

    @Override
    public List<VideoDetails> getVideosForUserId(@NonNull final String userId) {
        final TwitchHelixVideoResponse videoResponse = twitchHelixService.getVideoDetailsForUserId(
            userId);
        return videoResponse.getData().stream().map(vodData -> {
            Optional<VideoDetails> existingVideo = videoRepository.findById(vodData.getId());
            if (existingVideo.isEmpty()) {
                VideoDetails videoDetails = twitchVideoMapper.toVideoDetails(vodData);
                existingVideo = Optional.of(videoRepository.save(videoDetails));
            }
            return existingVideo.get();
        }).collect(Collectors.toList());
    }

    @Override
    public List<VideoDetails> getVideosForUsername(@NonNull final String username) {
        final TwitchHelixUserResponse userResponse = twitchHelixService.getUserDetailsForUsername(
            username);
        final Optional<TwitchHelixUserResponseData> userData = userResponse.getData().stream()
            .findFirst();
        return getVideosForUserId(userData.get().getId());
    }

    @Override
    @SneakyThrows
    @Async
    public CompletableFuture<Void> createVideoAnalysis(@NonNull String videoId) {
        log.info("Starting asynchronous analysis of video ID " + videoId);
        final VideoDetails videoDetails = getVideoByVideoId(videoId);

        logService.fetchLogsForDateRange(videoDetails.getChannelName(),
                videoDetails.getVideoStartTime(), videoDetails.getVideoEndTime())
            .subscribe(logs -> processVideoLogs(logs, videoDetails));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public List<VideoChatTimestamp> getVideoAnalysis(@NonNull String videoId) {
        return Streams.stream(videoChatTimestampRepository.findAll()).collect(Collectors.toList());
    }

    private void processVideoLogs(String chatLogs, final VideoDetails videoDetails) {
        Arrays.stream(Objects.requireNonNull(chatLogs)
                .split("\n"))
            .map(
                logLine -> LogParseUtils.buildChatLogRecord(logLine, videoDetails.getChannelName()))
            .filter((logRecord) -> Objects.nonNull(logRecord) && LogParseUtils.isWithinTimestamp(
                videoDetails.getVideoStartTime(), videoDetails.getVideoEndTime(),
                logRecord.getLogTimestamp()))
            .forEach(logRecord -> {
                final Map<String, Long> metrics = logService.countEmotes(logRecord.getChatText());
                createOrUpdateTimestamp(logRecord.getLogTimestamp(), videoDetails, metrics);
            });
    }

    private void createOrUpdateTimestamp(final LocalDateTime logTimestamp,
        final VideoDetails videoDetails, final Map<String, Long> metrics) {
        // Remove any non-zero values
        metrics.values()
            .removeIf(v -> v == 0L);

        // If all metrics were zero, skip the record
        if (metrics.values()
            .stream()
            .noneMatch(v -> v > 0L)) {
            return;
        }

        String hashKey;
        try {
            hashKey = videoDetails.getId() + "-" + objectMapper.writeValueAsString(logTimestamp);
        } catch (JsonProcessingException e) {
            log.error("Cannot serialize the date - skipping record");
            return;
        }

        var videostampRecord = videoChatTimestampRepository.findById(hashKey);
        if (videostampRecord.isPresent()) {
            // If a record already exists, merge the new metrics in and update the
            // existing record
            final VideoChatTimestamp existingTimestamp = videostampRecord.get();
            metrics.forEach((k, v) -> existingTimestamp.getChatMetrics()
                .merge(k, v, Long::sum));
        } else {
            // Else, create a new record
            videostampRecord = Optional.of(VideoChatTimestamp.builder()
                .id(hashKey)
                .timestamp(logTimestamp)
                .channelName(videoDetails.getChannelName())
                .videoId(videoDetails.getId())
                .chatMetrics(metrics)
                .timestampUrl(videoDetails.getVideoUrl() + "?t="
                    + LogParseUtils.getVideoTimestampString(videoDetails.getVideoStartTime(),
                    logTimestamp))
                .build());
        }

        videoChatTimestampRepository.save(videostampRecord.get());
    }
}