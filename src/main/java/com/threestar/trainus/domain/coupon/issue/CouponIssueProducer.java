package com.threestar.trainus.domain.coupon.issue;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponIssueProducer {

	private final StringRedisTemplate stringRedisTemplate;

	public boolean send(Long couponId, Long userId) {
		String stockKey = CouponIssueStreamConstant.STOCK_PREFIX + couponId;

		// Redis 원자 연산으로 재고 차감
		Long stock = stringRedisTemplate.opsForValue().decrement(stockKey);

		// 예외 및 재고 소진 처리
		if (stock == null) {
			return false;
		}

		if (stock < 0) {
			stringRedisTemplate.opsForValue().increment(stockKey);
			return false;
		}

		// Stream 메시지 생성 및 전송
		Map<String, String> content = new HashMap<>();
		content.put("couponId", String.valueOf(couponId));
		content.put("userId", String.valueOf(userId));

		stringRedisTemplate.opsForStream()
			.add(CouponIssueStreamConstant.STREAM_KEY, content);

		return true;
	}
}
