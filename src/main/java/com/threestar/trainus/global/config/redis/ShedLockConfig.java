package com.threestar.trainus.global.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

	@Bean
	public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
		// Redis를 이용한 분산 락 프로바이더 설정
		return new RedisLockProvider(connectionFactory);
	}
}
