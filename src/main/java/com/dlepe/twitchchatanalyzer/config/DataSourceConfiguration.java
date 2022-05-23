package com.dlepe.twitchchatanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

@Configuration
public class DataSourceConfiguration {

    @Bean
    public JedisConnectionFactory redisConnectionFactory(
        @Value("${redis.hostname}") String hostname,
        @Value("${redis.port}") int port, @Value("${redis.password}") String password) {
        final RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(hostname,
            port);
        config.setPassword(password);
        return new JedisConnectionFactory(config);
    }
}
