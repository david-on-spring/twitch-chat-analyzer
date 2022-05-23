package com.dlepe.twitchchatanalyzer.service.impl;

import com.dlepe.twitchchatanalyzer.exception.NoSuchElementException;
import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import com.dlepe.twitchchatanalyzer.model.mapper.TwitchVideoMapper;
import com.dlepe.twitchchatanalyzer.repository.VideoDetailsRepository;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.dlepe.twitchchatanalyzer.service.TwitchHelixService;
import com.dlepe.twitchchatanalyzer.service.VideoService;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.model.TwitchHelixVideoResponse;
import io.swagger.model.TwitchHelixVideoResponseData;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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
    private final TwitchVideoMapper twitchVideoMapper;

    @Override
    public VideoDetails getVideoByVideoId(@NonNull final String videoId) throws Exception {
        Optional<VideoDetails> videoDetails = videoRepository.findById(videoId);
        if (!videoDetails.isPresent()) {
            final TwitchHelixVideoResponse videoResponse = twitchHelixService.getVideoDetailsForVideoId(
                videoId);
            final Optional<TwitchHelixVideoResponseData> vodData = videoResponse.getData().stream()
                .filter(v -> videoId.equals(v.getId())).findFirst();
            if (vodData.isPresent()) {
                videoDetails = Optional.of(twitchVideoMapper.toVideoDetails(vodData.get()));
                videoRepository.save(videoDetails.get());
            } else {
                throw new NoSuchElementException("Cannot find a video for the specified ID");
            }
        }
        return videoDetails.get();
    }

    @Override
    public List<VideoDetails> getVideosForUserId(@NonNull final String userId) {
        final TwitchHelixVideoResponse videoResponse = twitchHelixService.getVideoDetailsForUserId(
            userId);
        return videoResponse.getData()
            .stream()
            .map(vodData -> {
                VideoDetails videoDetails = twitchVideoMapper.toVideoDetails(vodData);
                videoRepository.save(videoDetails);
                return videoDetails;
            })
            .collect(Collectors.toList());
    }

    @Override
    @SneakyThrows
    @Async
    public void analyzeVideoByVideoId(@NonNull String videoId) {
        log.info("Starting asynchronous analysis of video ID " + videoId);
        VideoDetails videoDetails = getVideoByVideoId(videoId);

        // Call logService to fetch and parse logs
        logService.parseChatLogs(videoDetails,
            logService.getRawLogDataForVideo(videoDetails));
    }

    @VisibleForTesting
    protected String getVideoTimestamp(final LocalDateTime startTime,
        final LocalDateTime specifiedTime) {
        final Duration duration = Duration.between(startTime, specifiedTime);
        final Long totalSecondsDifference = duration.get(ChronoUnit.SECONDS);
        final Long hours = totalSecondsDifference / 3600;
        final Long minutes = (totalSecondsDifference % 3600) / 60;
        final Long seconds = totalSecondsDifference % 60;

        return String.format("%dh%dm%ds", hours, minutes, seconds);
    }

}