package com.dlepe.twitchchatanalyzer.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TwitchChatAnalyzerConfiguration {

    private static final String TWITCH_OAUTH_REGISTRATION_ID = "twitch";

    @Value("${twitch-chat-analyzer.log-api.base-url}")
    private String logApiBaseUrl;

    @Value("${twitch-chat-analyzer.twitch-helix-api.base-url}")
    private String twitchHelixBaseUrl;

    @Bean(name = "logsWebClient")
    public WebClient logsWebClient() {
        final int size = 64 * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
            .build();

        return WebClient.builder()
            .baseUrl(logApiBaseUrl)
            .exchangeStrategies(strategies)
            .build();
    }

    @DependsOn({"twitchClientRegistration"})
    @Bean(name = "twitchWebClient")
    WebClient twitchWebClient(
        @Qualifier("twitchClientRegistration") ReactiveClientRegistrationRepository registrationRepository) {
        ExchangeFilterFunction oauthFilterFunction = buildOAuthExchangeFilter(
            registrationRepository,
            TWITCH_OAUTH_REGISTRATION_ID);
        return WebClient.builder()
            .baseUrl(twitchHelixBaseUrl)
            .defaultHeader("Client-Id",
                registrationRepository
                    .findByRegistrationId(TWITCH_OAUTH_REGISTRATION_ID)
                    .block()
                    .getClientId())
            .filters(exchangeFilterFunctions -> {
                exchangeFilterFunctions.add(oauthFilterFunction);
            })
            .build();
    }

    private ServerOAuth2AuthorizedClientExchangeFilterFunction buildOAuthExchangeFilter(
        ReactiveClientRegistrationRepository clientRegistrations, final String registrationId) {
        InMemoryReactiveOAuth2AuthorizedClientService clientService = new InMemoryReactiveOAuth2AuthorizedClientService(
            clientRegistrations);
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            clientRegistrations, clientService);
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
            authorizedClientManager);
        oauth.setDefaultClientRegistrationId(registrationId);
        return oauth;
    }

    @Bean(name = "twitchClientRegistration")
    ReactiveClientRegistrationRepository getRegistration(
        @Value("${spring.security.oauth2.client.provider.twitch.token-uri}") String tokenUri,
        @Value("${spring.security.oauth2.client.registration.twitch.client-id}") String clientId,
        @Value("${spring.security.oauth2.client.registration.twitch.client-secret}") String clientSecret) {
        ClientRegistration registration = ClientRegistration
            .withRegistrationId(TWITCH_OAUTH_REGISTRATION_ID)
            .tokenUri(tokenUri)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .build();
        return new InMemoryReactiveClientRegistrationRepository(registration);
    }

}
