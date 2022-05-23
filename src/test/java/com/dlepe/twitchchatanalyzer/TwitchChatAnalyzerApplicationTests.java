package com.dlepe.twitchchatanalyzer;

import com.dlepe.twitchchatanalyzer.config.TwitchEmoteConfiguration;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TwitchChatAnalyzerApplicationTests {

    @Autowired
    private TwitchEmoteConfiguration twitchEmoteConfiguration;

    @Test
    void contextLoads() {
    }

    @Test
    void propertyBindingTest() {
        Assert.assertTrue(twitchEmoteConfiguration.getKeywords().get("humor").contains("OMEGALUL"));

    }

}
