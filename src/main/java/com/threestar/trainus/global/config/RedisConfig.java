package com.threestar.trainus.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Value("${spring.data.redis.host}")
	private String host;

	@Value("${spring.data.redis.port}")
	private int port;

	@Value("${spring.data.redis.username:}")
	private String username;

	@Value("${spring.data.redis.password:}")
	private String password;
	/**
	 * 로컬 테스트용 프리픽스 -> spring.data.redis.key.prefix:redis://
	 * */
	@Value("${spring.data.redis.key.prefix}")
	private String prefix;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		SingleServerConfig singleServerConfig = config.useSingleServer();

		singleServerConfig.setAddress(prefix + host + ":" + port);

		// username과 password가 실제 값이 있을 때만 설정 (local에선 null로 테스트 요망)
		if (username != null && !username.isEmpty()) {
			singleServerConfig.setUsername(username);
		}
		if (password != null && !password.isEmpty()) {
			singleServerConfig.setPassword(password);
		}
		return Redisson.create(config);
	}

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		redisStandaloneConfiguration.setHostName(host);
		redisStandaloneConfiguration.setPort(port);
		redisStandaloneConfiguration.setUsername(username);
		redisStandaloneConfiguration.setPassword(password);
		return new LettuceConnectionFactory(redisStandaloneConfiguration);
	}

	@Bean
	@Primary
	public RedisTemplate<String, String> redisTemplate() {
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new StringRedisSerializer());
		redisTemplate.setConnectionFactory(redisConnectionFactory());
		return redisTemplate;
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate(
		RedisConnectionFactory redisConnectionFactory) {
		return new StringRedisTemplate(redisConnectionFactory);
	}
}
