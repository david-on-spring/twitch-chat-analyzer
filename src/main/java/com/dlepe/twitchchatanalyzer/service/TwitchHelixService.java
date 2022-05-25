package com.dlepe.twitchchatanalyzer.service;

import io.swagger.model.TwitchHelixUserResponse;
import io.swagger.model.TwitchHelixVideoResponse;

public interface TwitchHelixService {

    // Video Service
    TwitchHelixVideoResponse getVideoDetailsForUserId(final String userId);

    TwitchHelixVideoResponse getVideoDetailsForUsername(final String username);

    TwitchHelixVideoResponse getVideoDetailsForVideoId(final String videoId);

    // User Service
    TwitchHelixUserResponse getUserDetailsForUserId(final String userId);

    // Clips Service

}
