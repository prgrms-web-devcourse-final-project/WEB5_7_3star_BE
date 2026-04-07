package com.threestar.trainus.global.config;

import java.time.Duration;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Configuration
public class RedisConfig {

	// Core Redis 설정 클래스
	@Getter
	@Setter
	@Component
	@ConfigurationProperties(prefix = "spring.data.redis.core")
	public static class CoreRedisProperties {
		private String host;
		private int port;
		private String username;
		private String password;
	}

	// MQ Redis 설정 클래스
	@Getter
	@Setter
	@Component
	@ConfigurationProperties(prefix = "spring.data.redis.mq")
	public static class MQRedisProperties {
		private String host;
		private int port;
		private String username;
		private String password;
	}

	@Value("${spring.data.redis.key.prefix:redis://}")
	private String prefix;

	@Bean(name = "coreConnectionFactory")
	@Primary
	public RedisConnectionFactory coreConnectionFactory(CoreRedisProperties properties) {
		return createConnectionFactory(properties.getHost(), properties.getPort(), properties.getUsername(), properties.getPassword());
	}

	@Bean(name = "mqConnectionFactory")
	public RedisConnectionFactory mqConnectionFactory(MQRedisProperties properties) {
		return createConnectionFactory(properties.getHost(), properties.getPort(), properties.getUsername(), properties.getPassword());
	}

	private RedisConnectionFactory createConnectionFactory(String host, int port, String user, String pass) {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
		if (user != null && !user.isEmpty()) config.setUsername(user);
		if (pass != null && !pass.isEmpty()) config.setPassword(pass);

		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
			.commandTimeout(Duration.ofSeconds(5))
			.build();
			
		return new LettuceConnectionFactory(config, clientConfig);
	}

	@Bean(name = "coreRedisTemplate")
	@Primary // 기존 코드가 core를 기본으로 사용하도록 설정
	public StringRedisTemplate coreRedisTemplate(@Qualifier("coreConnectionFactory") RedisConnectionFactory factory) {
		return new StringRedisTemplate(factory);
	}

	@Bean(name = "mqRedisTemplate")
	public StringRedisTemplate mqRedisTemplate(@Qualifier("mqConnectionFactory") RedisConnectionFactory factory) {
		return new StringRedisTemplate(factory);
	}

	// Redisson (Core Redis 사용 - 분산 락 용도)
	@Bean
	public RedissonClient redissonClient(CoreRedisProperties properties) {
		Config config = new Config();
		SingleServerConfig singleServerConfig = config.useSingleServer();
		singleServerConfig.setAddress(prefix + properties.getHost() + ":" + properties.getPort());

		if (properties.getUsername() != null && !properties.getUsername().isEmpty()) {
			singleServerConfig.setUsername(properties.getUsername());
		}
		if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
			singleServerConfig.setPassword(properties.getPassword());
		}
		return Redisson.create(config);
	}
}
