package com.dlepe.twitchchatanalyzer.controller;

import com.dlepe.twitchchatanalyzer.model.VideoChatTimestamp;
import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import com.dlepe.twitchchatanalyzer.service.VideoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {

	private final VideoService videoService;

	@GetMapping("/{userId}/videos")
	public List<VideoDetails> getVideosForUserId(@PathVariable final String userId) {
		return videoService.getVideosForUserId(userId);
	}

	@GetMapping("/{videoId}")
	@SneakyThrows
	public VideoDetails getVideoDetailsForVideoId(@PathVariable final String videoId) {
		return videoService.getVideoByVideoId(videoId);
	}

	@PostMapping("/{userId}/videos/analysis")
	@SneakyThrows
	public ResponseEntity<Void> analyzeAllVideos(@PathVariable final String userId) {
		List<VideoDetails> videoDetails = videoService.getVideosForUserId(userId);
		videoDetails.stream().filter(vod -> !vod.isIndexed())
			.forEach(video -> videoService.createVideoAnalysis(video.getId()));
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{videoId}/analysis")
	@SneakyThrows
	public ResponseEntity<Void> createVideoAnalysis(@PathVariable final String videoId) {
		videoService.createVideoAnalysis(videoId);
		return ResponseEntity.ok().build();
	}


	@GetMapping("/{videoId}/analysis")
	@SneakyThrows
	public ResponseEntity<List<VideoChatTimestamp>> getVideoAnalysis(
		@PathVariable final String videoId) {
		return ResponseEntity.ok(videoService.getVideoAnalysis(videoId));
	}
}
