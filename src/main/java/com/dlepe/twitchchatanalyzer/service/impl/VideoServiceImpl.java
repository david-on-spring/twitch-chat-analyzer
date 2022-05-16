package com.dlepe.twitchchatanalyzer.service.impl;

import com.dlepe.twitchchatanalyzer.config.WebClientLoggingFilter;
import com.dlepe.twitchchatanalyzer.service.VideoService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.swagger.model.TwitchVod;
import reactor.core.publisher.Mono;

@Component
public class VideoServiceImpl implements VideoService {

        private final WebClient twitchWebClient;

        @Value("${twitch-chat-analyzer.twitch-helix-api.base-url}")
        private String twitchHelixBaseUrl;

        public VideoServiceImpl(final WebClient.Builder webClientBuilder,
                        final ServerOAuth2AuthorizedClientExchangeFilterFunction twitchOAuthExchangeFilter,
                        final ExchangeFilterFunction logRequest,
                        @Value("${twitch-chat-analyzer.twitch-helix-api.base-url}") String baseUrl) {
                this.twitchWebClient = webClientBuilder
                                .baseUrl(baseUrl)
                                .defaultHeader("Client-Id", "client-id-here")
                                .filters(exchangeFilterFunctions -> {
                                        exchangeFilterFunctions.add(twitchOAuthExchangeFilter);
                                        exchangeFilterFunctions.add(WebClientLoggingFilter.logRequest());
                                })
                                .build();
        }

        @Override
        public void getVideo(final String videoId) {
                final TwitchVod videoDetails = getVideoDetails(videoId);
        }

        private TwitchVod getVideoDetails(final String videoId) {
                final Mono<TwitchVod> response = twitchWebClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/videos")
                                                .queryParam("id", videoId)
                                                .build(videoId))
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(TwitchVod.class);
                return response.block();
        }

}