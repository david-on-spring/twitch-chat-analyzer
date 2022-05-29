package com.dlepe.twitchchatanalyzer.service.impl;

import com.dlepe.twitchchatanalyzer.TestUtils;
import com.dlepe.twitchchatanalyzer.model.VideoChatTimestamp;
import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import com.dlepe.twitchchatanalyzer.model.mapper.TwitchVideoMapper;
import com.dlepe.twitchchatanalyzer.repository.VideoChatTimestampRepository;
import com.dlepe.twitchchatanalyzer.repository.VideoDetailsRepository;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.dlepe.twitchchatanalyzer.service.TwitchHelixService;
import com.dlepe.twitchchatanalyzer.service.VideoService;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.model.TwitchHelixUserResponse;
import io.swagger.model.TwitchHelixUserResponseData;
import io.swagger.model.TwitchHelixVideoResponse;
import io.swagger.model.TwitchHelixVideoResponseData;
import io.swagger.model.TwitchHelixVideoResponsePagination;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
public class VideoServiceImplTest {

    @Mock
    private LogService mockLogService;
    @Mock
    private TwitchHelixService mockHelixService;
    @Mock
    private VideoDetailsRepository mockVideoRepository;
    @Mock
    private VideoChatTimestampRepository mockTimestampRepo;
    @Mock
    private TwitchVideoMapper twitchVideoMapper;
    @Mock
    private ObjectMapper objectMapper;

    private VideoService videoService;

    @BeforeEach
    void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        videoService = new VideoServiceImpl(mockLogService, mockHelixService,
            mockVideoRepository, mockTimestampRepo,
            twitchVideoMapper, objectMapper);
    }

    @Test
    @SneakyThrows
    void getVideoByVideoIdFromHelix() {
        var expected = createVideoDetails();
        var helixVideoResponse = createTwitchHelixVideoResponse();

        Mockito.when(mockHelixService.getVideoDetailsForVideoId(expected.getId()))
            .thenReturn(helixVideoResponse);
        Mockito.when(twitchVideoMapper.toVideoDetails(helixVideoResponse.getData().get(0)))
            .thenReturn(expected);
        Mockito.when(mockVideoRepository.save(expected))
            .thenReturn(expected);

        final VideoDetails actual = videoService.getVideoByVideoId(expected.getId());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @SneakyThrows
    void getVideoByVideoIdFromStorage() {
        var expected = createVideoDetails();
        Mockito.when(mockVideoRepository.findById(expected.getId()))
            .thenReturn(Optional.of(expected));
        var actual = videoService.getVideoByVideoId(expected.getId());

        Mockito.verify(mockHelixService,
            Mockito.never()).getVideoDetailsForVideoId(expected.getId());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getVideosForUserId() {
        var expected = createVideoDetails();
        var helixVideoResponse = createTwitchHelixVideoResponse();

        Mockito.when(mockHelixService.getVideoDetailsForUserId("123"))
            .thenReturn(helixVideoResponse);
        Mockito.when(mockVideoRepository.findById(expected.getId()))
            .thenReturn(Optional.of(expected));

        List<VideoDetails> videoDetailList = videoService.getVideosForUserId("123");
        Assertions.assertEquals(expected, videoDetailList.get(0));
    }

    @Test
    void getVideosForUsername() {
        var expected = createVideoDetails();
        var userResponse = createTwitchHelixUserResponse();
        var helixVideoResponse = createTwitchHelixVideoResponse();

        Mockito.when(mockHelixService.getUserDetailsForUsername("channelName"))
            .thenReturn(userResponse);
        Mockito.when(mockHelixService.getVideoDetailsForUserId("123"))
            .thenReturn(helixVideoResponse);
        Mockito.when(mockVideoRepository.findById(expected.getId()))
            .thenReturn(Optional.of(expected));

        final List<VideoDetails> videoDetailList = videoService.getVideosForUsername("channelName");
        Assertions.assertEquals(expected, videoDetailList.get(0));
    }

    @Test
    void createVideoAnalysis() {
        var expected = createVideoDetails();

        Mockito.when(mockVideoRepository.findById(expected.getId()))
            .thenReturn(Optional.of(expected));
        Mockito.when(mockTimestampRepo.findById(Mockito.anyString()))
            .thenReturn(Optional.empty());
        Mockito.when(mockTimestampRepo.save(Mockito.any(VideoChatTimestamp.class)))
            .thenReturn(Mockito.mock(VideoChatTimestamp.class));
        Mockito.when(mockLogService.fetchLogsForDateRange(expected.getChannelName(),
                expected.getVideoStartTime(), expected.getVideoEndTime()))
            .thenReturn(Flux.just(TestUtils.getTestChatLogs("valid_chat_logs.txt")));
        Mockito.when(mockLogService.countEmotes(Mockito.anyString()))
            .thenReturn(Map.of("humor", 30L));

        videoService.createVideoAnalysis(expected.getId());
        Mockito.verify(mockTimestampRepo, Mockito.times(16))
            .save(Mockito.any(VideoChatTimestamp.class));
    }

    @Test
    void getVideoAnalysis() {
    }

    private VideoDetails createVideoDetails() {
        return VideoDetails.builder()
            .id("123")
            .channelName("channelName")
            .videoTitle("the best stream ever")
            .videoUrl("the url")
            .indexed(true)
            .videoStartTime(LocalDateTime.of(2022, 5, 19, 0, 1, 0))
            .videoEndTime(LocalDateTime.of(2022, 5, 19, 23, 30, 50))
            .bestMoments(Map.of("humor", "humor url"))
            .build();
    }

    private TwitchHelixVideoResponse createTwitchHelixVideoResponse() {
        var response = new TwitchHelixVideoResponse();

        var videoData = new TwitchHelixVideoResponseData();
        videoData.setCreatedAt(
            OffsetDateTime.of(LocalDateTime.of(2022, 5, 19, 0, 0, 0),
                OffsetDateTime.now().getOffset()));
        videoData.setId("123");
        videoData.setDuration("23h59m59s");
        videoData.setUrl("the url");
        videoData.setUserName("mizkif");

        response.setData(List.of(videoData));
        response.setPagination(new TwitchHelixVideoResponsePagination());
        return response;
    }

    private TwitchHelixUserResponse createTwitchHelixUserResponse() {
        var response = new TwitchHelixUserResponse();

        var userData = new TwitchHelixUserResponseData();
        userData.setId("123");
        userData.setLogin("channelName");

        response.setData(List.of(userData));
        return response;
    }
}
