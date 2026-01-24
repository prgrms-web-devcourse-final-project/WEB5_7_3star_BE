package com.threestar.trainus.domain.coupon.issue;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

	private final StringRedisTemplate redisTemplate;
	private final CouponIssueService couponIssueService;

	private static final String STREAM_KEY = "coupon.issue.stream";
	private static final String GROUP = "coupon.issue.group";
	private static final String CONSUMER = "consumer-" + UUID.randomUUID();
	private static final String STOCK_KEY = "coupon:stock:";

	@PostConstruct
	public void init() {
		createGroupIfNotExists();
		startConsumerThread();
	}

	private void startConsumerThread() {
		Thread consumerThread = new Thread(() -> {
			while (true) {
				try {
					consumeNewMessages();
					handlePendingMessages();

				} catch (Exception e) {

				}
			}
		});

		consumerThread.setName("coupon-stream-consumer-thread");
		consumerThread.start();
	}

	private void consumeNewMessages() {
		List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream().read(
			Consumer.from(GROUP, CONSUMER),
			StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
			StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
		);

		if (messages == null || messages.isEmpty())
			return;

		for (MapRecord<String, Object, Object> message : messages) {
			process(message);
		}
	}

	private void handlePendingMessages() {
		// pending이 없으면 skip 가능하도록
		PendingMessagesSummary pending = redisTemplate.opsForStream()
			.pending(STREAM_KEY, GROUP);

		if (pending == null || pending.getTotalPendingMessages() == 0)
			return;

		List<MapRecord<String, Object, Object>> pendingList =
			redisTemplate.opsForStream().read(
				Consumer.from(GROUP, CONSUMER),
				StreamReadOptions.empty().count(10),
				StreamOffset.create(STREAM_KEY, ReadOffset.from("0"))
			);

		if (pendingList == null)
			return;

		for (MapRecord<String, Object, Object> message : pendingList) {
			process(message);
		}
	}

	private void process(MapRecord<String, Object, Object> message) {
		Long couponId = null;
		Long userId = null;
		try {
			couponId = Long.valueOf(message.getValue().get("couponId").toString());
			userId = Long.valueOf(message.getValue().get("userId").toString());

			log.info("CONSUME coupon={} user={}", couponId, userId);

			boolean issued = couponIssueService.issue(couponId, userId);

			if (issued) {
				//정상 발급, DB quantity 감소처리
				redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, message.getId());
				return;
			}

			//발급 오류
			rollbackStock(couponId);
			redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, message.getId());
		} catch (Exception e) {
			log.error("Issue failed: {}, message stays in PENDING", e.getMessage());
		}
	}

	private void rollbackStock(Long couponId) {
		String stockKey = STOCK_KEY + couponId;
		redisTemplate.opsForValue().increment(stockKey);
	}

	private void createGroupIfNotExists() {
		try {
			redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP);
			log.info("Redis stream group created");
		} catch (Exception e) {
			// 그룹 이미 있으면 예외 발생함 (무시)
			log.info("Redis stream group already exists");
		}
	}

	public void testConsumeOnce() {
		try {
			consumeNewMessages();
			handlePendingMessages();
		} catch (Exception e) {
			log.error("Manual consume failed: {}", e.getMessage());
		}
	}
}
