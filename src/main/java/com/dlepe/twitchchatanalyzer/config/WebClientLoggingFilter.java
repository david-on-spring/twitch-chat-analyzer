package com.dlepe.twitchchatanalyzer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

@Slf4j
public class WebClientLoggingFilter {

    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            logMethodAndUrl(request);
            logHeaders(request);

            return Mono.just(request);
        });
    }

    private static void logMethodAndUrl(ClientRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.method().name());
        sb.append(" to ");
        sb.append(request.url());

        log.debug(sb.toString());
    }

    private static void logHeaders(ClientRequest request) {
        request.headers().forEach((name, values) -> {
            values.forEach(value -> {
                logNameAndValuePair(name, value);
            });
        });
    }

    private static void logNameAndValuePair(String name, String value) {
        log.debug("{}={}", name, value);
    }

    public static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            logStatus(response);
            logHeaders(response);

            return logBody(response);
        });
    }

    private static void logStatus(ClientResponse response) {
        HttpStatus status = response.statusCode();
        log.debug("Returned status code {} ({})", status.value(), status.getReasonPhrase());
    }

    private static void logHeaders(ClientResponse response) {
        response.headers().asHttpHeaders().forEach((name, values) -> {
            values.forEach(value -> {
                logNameAndValuePair(name, value);
            });
        });
    }

    private static Mono<ClientResponse> logBody(ClientResponse response) {
        if (response.statusCode() != null
            && (response.statusCode().is4xxClientError() || response.statusCode()
            .is5xxServerError())) {
            return response.bodyToMono(String.class)
                .flatMap(body -> {
                    log.debug("Body is {}", body);
                    return Mono.just(response);
                });
        } else {
            return Mono.just(response);
        }
    }
}