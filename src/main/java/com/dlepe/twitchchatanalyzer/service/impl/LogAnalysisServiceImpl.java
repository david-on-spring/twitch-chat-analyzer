package com.dlepe.twitchchatanalyzer.service.impl;

import java.time.OffsetDateTime;

import com.dlepe.twitchchatanalyzer.service.LogAnalysisService;

import io.swagger.model.TwitchVodData;

public class LogAnalysisServiceImpl implements LogAnalysisService {

    @Override
    public void analyzeVideoDetails(TwitchVodData videoData) {
        final OffsetDateTime videoCreationTime = videoData.getCreatedAt();

        final String videoDuration = videoData.getDuration();
        // TODO Auto-generated method stub

    }

}
