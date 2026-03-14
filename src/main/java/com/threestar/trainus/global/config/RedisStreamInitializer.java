package com.threestar.trainus.global.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.threestar.trainus.domain.coupon.issue.CouponIssueStreamConstant;
import com.threestar.trainus.domain.lesson.issue.LessonApplyStreamConstant;

import jakarta.annotation.PostConstruct;

@Component
public class RedisStreamInitializer {

	private static final String COUPON_STREAM_KEY = CouponIssueStreamConstant.STREAM_KEY;
	private static final String COUPON_GROUP_NAME = CouponIssueStreamConstant.GROUP;

	private static final String LESSON_STREAM_KEY = LessonApplyStreamConstant.STREAM_KEY;
	private static final String LESSON_GROUP_NAME = LessonApplyStreamConstant.GROUP;

	private final StringRedisTemplate stringRedisTemplate;

	RedisStreamInitializer(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@PostConstruct
	public void init() {
		// 쿠폰 그룹 초기화
		initGroup(COUPON_STREAM_KEY, COUPON_GROUP_NAME);
		// 레슨 그룹 초기화
		initGroup(LESSON_STREAM_KEY, LESSON_GROUP_NAME);
	}

	private void initGroup(String streamKey, String groupName) {
		try {
			stringRedisTemplate.opsForStream()
				.createGroup(streamKey, groupName);
		} catch (Exception e) {
			// 이미 존재하는 경우 무시
		}
	}
}
