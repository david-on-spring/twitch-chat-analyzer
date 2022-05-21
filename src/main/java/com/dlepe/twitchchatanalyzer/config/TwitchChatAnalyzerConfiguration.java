package com.dlepe.twitchchatanalyzer.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
                                .filters(exchangeFilterFunctions -> {
                                        exchangeFilterFunctions.add(WebClientLoggingFilter.logRequest());
                                        exchangeFilterFunctions.add(WebClientLoggingFilter.logResponse());
                                })
                                .build();
        }

        @DependsOn({ "twitchClientRegistration" })
        @Bean(name = "twitchWebClient")
        WebClient twitchWebClient(
                        @Qualifier("twitchClientRegistration") ReactiveClientRegistrationRepository registrationRepository) {
                ExchangeFilterFunction oauthFilterFunction = buildOAuthExchangeFilter(registrationRepository,
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
                                        exchangeFilterFunctions.add(WebClientLoggingFilter.logRequest());
                                        exchangeFilterFunctions.add(WebClientLoggingFilter.logResponse());
                                })
                                .build();
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

        @Bean
        public ObjectMapper objectMapper() {
                ObjectMapper mapper = new ObjectMapper();

                // Don't throw an exception when json has extra fields you are
                // not serializing on. This is useful when you want to use a pojo
                // for deserialization and only care about a portion of the json
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                // Ignore null values when writing json.
                mapper.setSerializationInclusion(Include.NON_NULL);

                // Write times as a String instead of a Long so its human readable.
                mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                mapper.registerModule(new JavaTimeModule());

                return mapper;
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

}
