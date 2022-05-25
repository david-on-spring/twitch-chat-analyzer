package com.dlepe.twitchchatanalyzer.config;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "twitch-chat-analyzer")
public class TwitchEmoteConfiguration {

    @Getter
    @Setter
    private Map<String, List<String>> keywords;
}