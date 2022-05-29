package com.dlepe.twitchchatanalyzer.service.impl;

import com.dlepe.twitchchatanalyzer.service.TwitchHelixService;
import io.swagger.model.TwitchHelixUserResponse;
import io.swagger.model.TwitchHelixUserResponseData;
import io.swagger.model.TwitchHelixVideoResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class TwitchHelixServiceImpl implements TwitchHelixService {

    private final WebClient twitchWebClient;

    @Override
    public TwitchHelixVideoResponse getVideoDetailsForUserId(String userId) {
        return getVideoDetails("user_id", userId).block();
    }

    @Override
    public TwitchHelixVideoResponse getVideoDetailsForUsername(String username) {
        final Optional<TwitchHelixUserResponseData> userData = getUserDetailsForUserId(
            username).getData().stream().findFirst();
        return getVideoDetailsForUserId(userData.get().getId());
    }

    @Override
    public TwitchHelixVideoResponse getVideoDetailsForVideoId(String videoId) {
        return getVideoDetails("id", videoId).block();
    }

    @Override
    public TwitchHelixUserResponse getUserDetailsForUserId(String userId) {
        return getUserDetails("id", userId).block();
    }

    @Override
    public TwitchHelixUserResponse getUserDetailsForUsername(String username) {
        return getUserDetails("login", username).block();
    }

    private Mono<TwitchHelixVideoResponse> getVideoDetails(@NonNull final String paramName,
        @NonNull final String paramValue) {
        return twitchWebClient.get().uri(
                uriBuilder -> uriBuilder.path("/videos").queryParam(paramName, paramValue)
                    .build(paramValue)).accept(MediaType.APPLICATION_JSON).retrieve()
            .bodyToMono(TwitchHelixVideoResponse.class);
    }

    private Mono<TwitchHelixUserResponse> getUserDetails(@NonNull final String paramName,
        @NonNull final String paramValue) {
        return twitchWebClient.get().uri(
                uriBuilder -> uriBuilder.path("/users").queryParam(paramName, paramValue)
                    .build(paramValue)).accept(MediaType.APPLICATION_JSON).retrieve()
            .bodyToMono(TwitchHelixUserResponse.class);
    }

}
