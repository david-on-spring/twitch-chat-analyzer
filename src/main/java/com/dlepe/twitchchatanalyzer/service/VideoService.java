package com.dlepe.twitchchatanalyzer.service;

import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import java.util.List;
import lombok.NonNull;

public interface VideoService {

    VideoDetails getVideoByVideoId(@NonNull final String videoId) throws Exception;

    List<VideoDetails> getVideosForUserId(@NonNull final String userId);

    void analyzeVideoByVideoId(@NonNull final String videoId);

}
