package com.dlepe.twitchchatanalyzer.service.impl;

import com.dlepe.twitchchatanalyzer.model.mapper.TwitchVideoMapper;
import com.dlepe.twitchchatanalyzer.repository.VideoChatTimestampRepository;
import com.dlepe.twitchchatanalyzer.repository.VideoDetailsRepository;
import com.dlepe.twitchchatanalyzer.service.LogService;
import com.dlepe.twitchchatanalyzer.service.TwitchHelixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VideoServiceImplTest {

    @Mock
    private LogService mockLogService;

    @Mock
    private VideoDetailsRepository mockVideoRepository;

    @Mock
    private VideoChatTimestampRepository mockChatRepository;

    @Mock
    private TwitchHelixService mockHelixService;

    @Mock
    private TwitchVideoMapper twitchVideoMapper;

    private VideoServiceImpl videoAnalysisService;

    @BeforeEach
    void setup() {
        videoAnalysisService = new VideoServiceImpl(mockLogService, mockHelixService,
            mockVideoRepository, mockChatRepository,
            twitchVideoMapper);
    }
}
