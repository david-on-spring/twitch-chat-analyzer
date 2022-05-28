package com.dlepe.twitchchatanalyzer.service.impl;

import com.dlepe.twitchchatanalyzer.exception.NoSuchElementException;
import com.dlepe.twitchchatanalyzer.model.VideoChatTimestamp;
import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import com.dlepe.twitchchatanalyzer.model.mapper.TwitchVideoMapper;
import com.dlepe.twitchchatanalyzer.repository.VideoChatTimestampRepository;
import com.dlepe.twitchchatanalyzer.repository.VideoDetailsRepository;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.dlepe.twitchchatanalyzer.service.TwitchHelixService;
import com.dlepe.twitchchatanalyzer.service.VideoService;
import com.google.common.collect.Streams;
import io.swagger.model.TwitchHelixVideoResponse;
import io.swagger.model.TwitchHelixVideoResponseData;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

	private final LogService logService;
	private final TwitchHelixService twitchHelixService;
	private final VideoDetailsRepository videoRepository;
	private final VideoChatTimestampRepository videoChatTimestampRepository;
	private final TwitchVideoMapper twitchVideoMapper;

	@Override
	public VideoDetails getVideoByVideoId(@NonNull final String videoId) {
		Optional<VideoDetails> videoDetails = videoRepository.findById(videoId);
		if (videoDetails.isEmpty()) {
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
		return videoResponse.getData().stream().map(vodData -> {
			final Optional<VideoDetails> existingVideo = videoRepository.findById(vodData.getId());
			if (existingVideo.isPresent()) {
				return existingVideo.get();
			}
			VideoDetails videoDetails = twitchVideoMapper.toVideoDetails(vodData);
			videoRepository.save(videoDetails);
			return videoDetails;
		}).collect(Collectors.toList());
	}

	@Override
	@SneakyThrows
	@Async
	public CompletableFuture<Void> createVideoAnalysis(@NonNull String videoId) {
		log.info("Starting asynchronous analysis of video ID " + videoId);
		final VideoDetails videoDetails = getVideoByVideoId(videoId);

			// Call logService to fetch and parse logs
			final Map<String, String> bestMoments = logService.parseChatLogs(videoDetails,
					logService.getRawLogDataForVideo(videoDetails));

			// Update the video details
			videoDetails.setIndexed(true);
			videoDetails.setBestMoments(bestMoments);
			videoRepository.save(videoDetails);

		log.info("Completed analysis of video ID " + videoId);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public List<VideoChatTimestamp> getVideoAnalysis(@NonNull String videoId) {
		return Streams.stream(videoChatTimestampRepository.findAll()).collect(Collectors.toList());
	}

}