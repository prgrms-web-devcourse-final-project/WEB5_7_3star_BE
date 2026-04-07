package com.threestar.trainus.global.config.redis;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile("consumer")
@Configuration
public class RedisStreamConfig {

	@Value("${spring.data.redis.stream.threads.core:5}")
	private int corePoolSize;

	@Value("${spring.data.redis.stream.threads.max:10}")
	private int maxPoolSize;

	@Bean
	public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
		@Qualifier("mqConnectionFactory") RedisConnectionFactory factory) {

		// 스레드 풀 생성 크기 설정
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("stream-worker-");
		executor.initialize();

		log.info("Stream Consumer Thread Pool Initialized (MQ Redis): core={}, max={}", corePoolSize, maxPoolSize);

		StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
			StreamMessageListenerContainer.StreamMessageListenerContainerOptions
				.builder()
				.pollTimeout(Duration.ofMillis(500))
				.batchSize(1000)
				.executor(executor)
				.errorHandler(t -> log.warn("Redis Stream error: {}", t.getMessage()))
				.build();

		return StreamMessageListenerContainer.create(factory, options);
	}
}
