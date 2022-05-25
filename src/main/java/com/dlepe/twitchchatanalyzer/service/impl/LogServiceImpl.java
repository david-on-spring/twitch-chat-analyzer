package com.dlepe.twitchchatanalyzer.service.impl;

import com.dlepe.twitchchatanalyzer.config.TwitchEmoteConfiguration;
import com.dlepe.twitchchatanalyzer.dto.ChatLogRecord;
import com.dlepe.twitchchatanalyzer.model.VideoChatTimestamp;
import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import com.dlepe.twitchchatanalyzer.repository.VideoChatTimestampRepository;
import com.dlepe.twitchchatanalyzer.service.LogParseUtils;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

	@VisibleForTesting
	protected final static String LOGS_API_PATH = "/channel/{channelName}/{logYear}/{logMonth}/{logDay}/";

	private final WebClient logsWebClient;
	private final TwitchEmoteConfiguration configuredEmotes;
	private final VideoChatTimestampRepository videoChatTimestampRepository;
	private final ObjectMapper objectMapper;


	@Override
	public List<ChatLogRecord> getRawLogDataForDateRange(final String channelName,
		final LocalDateTime startTime, final LocalDateTime endTime) {
		return startTime.toLocalDate().datesUntil(endTime.toLocalDate().plusDays(1))
			.flatMap(day -> getRawLogData(channelName, day, startTime, endTime).stream())
			.collect(Collectors.toList());
	}

	private List<ChatLogRecord> getRawLogData(final String channelName, final LocalDate logsDate,
		final LocalDateTime startTime, final LocalDateTime endTime) {
		log.debug(String.format(
			"Fetching log data for [channelName=%s] [logDate=%s] [startTime=%s] [endTime=%s]",
			channelName, logsDate, startTime, endTime));
		final Mono<String> response = logsWebClient.get().uri(
				uriBuilder -> uriBuilder.path(LOGS_API_PATH)
					.build(channelName.toLowerCase(), logsDate.getYear(), logsDate.getMonthValue(),
						logsDate.getDayOfMonth())).accept(MediaType.TEXT_PLAIN).retrieve()
			.bodyToMono(String.class);

		// Optimize by grouping aggregating all chat text having similar timestamps
		// Also, consider skipping any chat text where chatText.length < smallest monitored emote
		return Arrays.asList(Objects.requireNonNull(response.block()).split("\n")).parallelStream()
			.map(logLine -> buildChatLogRecord(logLine, channelName)).filter(
				(logRecord) -> Objects.nonNull(logRecord) && (
					logRecord.getLogTimestamp().isAfter(startTime) || logRecord.getLogTimestamp()
						.isEqual(startTime)) && (logRecord.getLogTimestamp().isBefore(endTime)
					|| logRecord.getLogTimestamp().isEqual(endTime))).collect(Collectors.toList());
	}

	private ChatLogRecord buildChatLogRecord(final String logLine, final String channelName) {
		try {
			final String[] logParts = logLine.split(" #" + channelName + " ");
			final LocalDateTime logTimestamp = LogParseUtils.getTimestampToNearestMinute(
				logParts[0]);
			final String[] chatLogParts = logParts[1].split(":", 2);
			final String chatterUsername = chatLogParts[0];
			final String chatText = chatLogParts[1].strip();
			return ChatLogRecord.builder().channelName(channelName).username(chatterUsername)
				.chatText(chatText).logTimestamp(logTimestamp).build();
		} catch (Exception e) {
			return null;
		}
	}

	@VisibleForTesting
	protected Map<String, Long> parseChatMessage(final String chatText) {
		final Map<String, Long> emoteCountMap = new HashMap<>();

		configuredEmotes.getKeywords().keySet().forEach(emotion -> {
			emoteCountMap.compute(emotion, (key, value) -> {
				// Get the list of emotes mapped to the emotion
				final List<String> emotes = configuredEmotes.getKeywords().get(emotion);

				// Assign an emotion score by totaling the count of each occurrence of emote
				return emotes.stream().map(emote -> LogParseUtils.kmpSearch(emote, chatText))
					.reduce(0L, Long::sum);
			});
		});

		return emoteCountMap;
	}

	@Override
	public List<ChatLogRecord> getRawLogDataForVideo(VideoDetails videoDetails) {
		return getRawLogDataForDateRange(videoDetails.getChannelName(),
			videoDetails.getVideoStartTime(), videoDetails.getVideoEndTime());
	}

	// Not thread safe
	@Override
	@SneakyThrows
	public Map<String, String> parseChatLogs(final VideoDetails videoDetails,
		final List<ChatLogRecord> chatLogs) {
		final Map<String, String> bestMoments = new HashMap<>();
		AtomicReference<String> funniestMoment = new AtomicReference<>();
		final AtomicLong maxHumorScore = new AtomicLong(0L);

		chatLogs.forEach(logRecord -> {
			// Extract the occurrence of emotes in the given chat message
			final Map<String, Long> metrics = parseChatMessage(logRecord.getChatText());

			// Remove any non-zero values
			metrics.values().removeIf(v -> v < 1L);

			// If all metrics were zero, skip the record
			if (metrics.values().stream().noneMatch(v -> v > 0L)) {
				return;
			}

			final var timestamp = createOrUpdateTimestamp(logRecord.getLogTimestamp(), videoDetails,
				metrics);

			// Skip the loop if the timestamp comes back as null
			// TODO: update with error handling
			if (null == timestamp) {
				return;
			}

			final long humorScore = timestamp.getChatMetrics().getOrDefault("humor", 0L);
			if (humorScore > maxHumorScore.get()) {
				maxHumorScore.set(humorScore);
				bestMoments.put("humor", timestamp.getTimestampUrl());
			}
		});

		return bestMoments;
	}

	// Todo - move to video service?
	private VideoChatTimestamp createOrUpdateTimestamp(final LocalDateTime logTimestamp,
		final VideoDetails videoDetails, final Map<String, Long> metrics) {

		String hashKey;
		try {
			hashKey = videoDetails.getId() + "-" + objectMapper.writeValueAsString(logTimestamp);
		} catch (JsonProcessingException e) {
			log.error("Cannot serialize the date - skipping record");
			return null;
		}

		final Optional<VideoChatTimestamp> existingRecord = videoChatTimestampRepository.findById(
			hashKey);
		if (existingRecord.isPresent()) {
			// If a record already exists, merge the new metrics in and update the existing record
			final VideoChatTimestamp existingTimestamp = existingRecord.get();
			metrics.forEach((k, v) -> existingTimestamp.getChatMetrics().merge(k, v, Long::sum));
			return videoChatTimestampRepository.save(existingTimestamp);
		} else {
			// Else, create a new record
			var newRecord = VideoChatTimestamp.builder().id(hashKey).timestamp(logTimestamp)
				.channelName(videoDetails.getChannelName()).videoId(videoDetails.getId())
				.chatMetrics(metrics).timestampUrl(
					videoDetails.getVideoUrl() + "?t=" + LogParseUtils.getVideoTimestampString(
						videoDetails.getVideoStartTime(), logTimestamp)).build();
			return videoChatTimestampRepository.save(newRecord);
		}
	}
}
