package com.dlepe.twitchchatanalyzer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@Configuration
@EnableRedisRepositories
@EnableTransactionManagement
public class DataSourceConfiguration {

	private final String hostname;
	private final int port;

    public DataSourceConfiguration(final @Value("${redis.hostname}") String hostname,
		final @Value("${redis.port}") int port) {
		this.hostname = hostname;
		this.port = port;
    }

	@Bean
	public RedisTemplate<?, ?> redisTemplate() {
		final RedisTemplate<?, ?> template = new RedisTemplate<>();

		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setConnectionFactory(redisConnectionFactory());
		template.setEnableTransactionSupport(true);
		template.afterPropertiesSet();
		return template;
	}

	@Bean
	public JedisConnectionFactory redisConnectionFactory() {
		final RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(hostname,
			port);
		return new JedisConnectionFactory(config);
	}
}
