package com.threestar.trainus.global.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.threestar.trainus.domain.coupon.issue.CouponIssueStreamConstant;

import jakarta.annotation.PostConstruct;

@Component
public class RedisStreamInitializer {

	private static final String STREAM_KEY = CouponIssueStreamConstant.STREAM_KEY;
	private static final String GROUP_NAME = CouponIssueStreamConstant.GROUP;

	private final StringRedisTemplate stringRedisTemplate;

	RedisStreamInitializer(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@PostConstruct
	public void init() {
		try {
			stringRedisTemplate.opsForStream()
				.createGroup(STREAM_KEY, GROUP_NAME);
		} catch (Exception e) {

		}
	}
}
