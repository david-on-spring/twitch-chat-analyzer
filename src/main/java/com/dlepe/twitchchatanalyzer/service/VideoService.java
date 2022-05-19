package com.dlepe.twitchchatanalyzer.service;

import java.util.List;

import com.dlepe.twitchchatanalyzer.dto.TwitchAnalysisDTO.TwitchVideoAnalysis;

public interface VideoService {
    List<TwitchVideoAnalysis> getVideo(final String videoId);

}
