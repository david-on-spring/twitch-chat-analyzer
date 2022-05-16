package com.dlepe.twitchchatanalyzer.service;

import io.swagger.model.TwitchVodData;

public interface LogAnalysisService {

    public void analyzeVideoDetails(final TwitchVodData videoData);
}
