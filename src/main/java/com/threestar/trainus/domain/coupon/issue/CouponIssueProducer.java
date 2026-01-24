package com.threestar.trainus.domain.coupon.issue;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CouponIssueProducer {

	private static final String STREAM_KEY = CouponIssueStreamConstant.STREAM_KEY;
	private static final String STOCK_KEY_PREFIX = "coupon:stock:";

	private final StringRedisTemplate stringRedisTemplate;

	public CouponIssueProducer(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public boolean send(Long couponId, Long userId) {
		String stockKey = STOCK_KEY_PREFIX + couponId;

		Long stock = stringRedisTemplate.opsForValue().decrement(stockKey);

		if (stock == null) {
			return false;
		}

		if (stock < 0) {
			stringRedisTemplate.opsForValue().increment(stockKey);
			return false;
		}
		Map<String, String> message = new HashMap<>();
		message.put("couponId", couponId.toString());
		message.put("userId", userId.toString());

		stringRedisTemplate.opsForStream()
			.add(STREAM_KEY, message);
		return true;
	}
}
