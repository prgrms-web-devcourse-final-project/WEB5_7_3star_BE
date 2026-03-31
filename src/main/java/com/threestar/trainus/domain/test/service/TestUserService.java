package com.threestar.trainus.domain.test.service;

import com.threestar.trainus.domain.lesson.issue.LessonApplyStreamConstant;
import com.threestar.trainus.global.config.security.JwtProvider;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.profile.service.ProfileFacadeService;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestUserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ProfileFacadeService profileFacadeService;
	private final JdbcTemplate jdbcTemplate;
	private final StringRedisTemplate stringRedisTemplate;
	private final JwtProvider jwtProvider;

	@Transactional
	public void createUsers(int count) {
		log.info("Starting bulk user creation: {} users", count);

		// 유저 대량 생성
		// ON CONFLICT로 중복 생성 방지
		String userSql = "INSERT INTO users (email, password, nickname, role, test_user_id, created_at, updated_at) "
			+ "VALUES (?, ?, ?, ?, ?, NOW(), NOW()) " + "ON CONFLICT (email) DO NOTHING";

		String encodedPassword = passwordEncoder.encode("password");

		jdbcTemplate.batchUpdate(userSql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				long num = i + 1;
				ps.setString(1, "testuser" + num + "@example.com");
				ps.setString(2, encodedPassword);
				ps.setString(3, "testuser" + num);
				ps.setString(4, UserRole.USER.name());
				ps.setLong(5, num);
			}

			@Override
			public int getBatchSize() {
				return count;
			}
		});

		// 프로필 일괄 생성
		String profileSql = "INSERT INTO profile (user_id, profile_image, intro) "
			+ "SELECT u.id, null, 'Test user intro' FROM users u " + "LEFT JOIN profile p ON u.id = p.user_id "
			+ "WHERE u.email LIKE 'testuser%@example.com' AND p.id IS NULL";
		jdbcTemplate.execute(profileSql);

		// 메타데이터 일괄 생성
		String metadataSql =
			"INSERT INTO profile_metadata (user_id, review_count, rating) " + "SELECT u.id, 0, 0.0 FROM users u "
				+ "LEFT JOIN profile_metadata pm ON u.id = pm.user_id "
				+ "WHERE u.email LIKE 'testuser%@example.com' AND pm.id IS NULL";
		jdbcTemplate.execute(metadataSql);

		log.info("Bulk creation completed: Users, Profiles, and Metadata are ready.");
	}

	@Transactional(readOnly = true)
	public String generateTokenCsvForTestUsers() {
		log.info("Generating JWT Token CSV for test users...");
		StringBuilder csv = new StringBuilder("userId,accessToken\n");

		jdbcTemplate.query("SELECT id FROM users WHERE email LIKE 'testuser%@example.com' ORDER BY id ASC", rs -> {
			Long userId = rs.getLong("id");
			String accessToken = jwtProvider.createAccessToken(userId, "USER");
			csv.append(userId).append(",").append(accessToken).append("\n");
		});

		return csv.toString();
	}

	@Transactional
	public User findOrCreateUser(Long userId) {
		Optional<User> existingUser = userRepository.findById(userId);
		if (existingUser.isPresent()) {
			return existingUser.get();
		}
		String email = "testuser" + userId + "@example.com";
		String nickname = "testuser" + userId;

		if (userRepository.existsByEmail(email) || userRepository.existsByNickname(nickname)) {
			return userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("테스트 유저 생성 실패"));
		}

		String encodedPassword = passwordEncoder.encode("password");
		User newUser = User.builder()
			.email(email)
			.password(encodedPassword)
			.nickname(nickname)
			.role(UserRole.USER)
			.build();

		User savedUser = userRepository.save(newUser);
		profileFacadeService.createDefaultProfile(savedUser);

		return savedUser;
	}

	@Transactional
	public User findOrCreateUser2(Long userId) {

		return userRepository.findByTestUserId(userId).orElseGet(() -> {
			User user = User.builder()
				.testUserId(userId)
				.email("testuser" + userId + "@example.com")
				.nickname("testuser" + userId)
				.password(passwordEncoder.encode("password"))
				.role(UserRole.USER)
				.build();

			User saved = userRepository.save(user);
			profileFacadeService.createDefaultProfile(saved);
			return saved;
		});
	}

	@Transactional
	public void clearAllData() {
		log.info("Starting hard reset of all test data...");

		jdbcTemplate.execute(
			"TRUNCATE TABLE lesson_participants, lesson_applications, lesson_images, profile, profile_metadata, users RESTART IDENTITY CASCADE");

		clearRedisData();

		log.info("Hard reset completed successfully.");
	}

	public void clearRedisData() {
		log.info("Clearing lesson-related Redis data...");
		String streamKey = LessonApplyStreamConstant.STREAM_KEY;
		String groupName = LessonApplyStreamConstant.GROUP;

		// 데이터만 비우고 그룹은 유지
		try {
			stringRedisTemplate.opsForStream().trim(streamKey, 0);
			log.info("Redis Stream [{}] trimmed to 0", streamKey);

			// Pending 메세지 정리
			try {
				PendingMessages pendingMessages = stringRedisTemplate.opsForStream()
					.pending(streamKey, groupName, Range.unbounded(), 10000L);
				
				if (pendingMessages != null && !pendingMessages.isEmpty()) {
					RecordId[] ids = pendingMessages.stream()
						.map(PendingMessage::getId)
						.toArray(RecordId[]::new);
					stringRedisTemplate.opsForStream().acknowledge(streamKey, groupName, ids);
					log.info("PEL cleared: {} messages ACKed", ids.length);
				}
			} catch (Exception e) {
				log.warn("Failed to clear PEL: {}", e.getMessage());
			}

			// 그룹 오프셋 처음으로 리셋
			stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>)connection -> {
				try {
					connection.execute("XGROUP", "SETID".getBytes(), streamKey.getBytes(), groupName.getBytes(), "0-0".getBytes());
					log.info("Consumer Group [{}] offset reset to 0-0", groupName);
				} catch (Exception e) {
					log.warn("Failed to reset group offset: {}", e.getMessage());
				}
				return null;
			});

			// Dirty Set 초기화
			stringRedisTemplate.delete(LessonApplyStreamConstant.DIRTY_SET_KEY);
			log.info("Dirty Set [{}] cleared", LessonApplyStreamConstant.DIRTY_SET_KEY);
		} catch (Exception e) {
			log.warn("Redis reset failed: {}", e.getMessage());
		}

		// 중복 신청 방지 데이터 삭제 (lesson:apply:duplicate:*)
		java.util.Set<String> duplicateKeys = stringRedisTemplate.keys(LessonApplyStreamConstant.DUPLICATE_PREFIX + "*");
		if (duplicateKeys != null && !duplicateKeys.isEmpty()) {
			stringRedisTemplate.delete(duplicateKeys);
			log.info("Duplicate Filter keys [{}] cleared", duplicateKeys.size());
		}

		// 레슨 재고 데이터 삭제 (lesson:stock:*)
		java.util.Set<String> stockKeys = stringRedisTemplate.keys(LessonApplyStreamConstant.STOCK_PREFIX + "*");
		if (stockKeys != null && !stockKeys.isEmpty()) {
			stringRedisTemplate.delete(stockKeys);
		}

		// 신청 상태 데이터 삭제 (lesson:apply:status:*)
		java.util.Set<String> statusKeys = stringRedisTemplate.keys(LessonApplyStreamConstant.STATUS_PREFIX + "*");
		if (statusKeys != null && !statusKeys.isEmpty()) {
			stringRedisTemplate.delete(statusKeys);
		}

		// 기타 테스트 세션 등 정리
		java.util.Set<String> sessionKeys = stringRedisTemplate.keys("test:session:*");
		if (sessionKeys != null && !sessionKeys.isEmpty()) {
			stringRedisTemplate.delete(sessionKeys);
		}

		log.info("Redis data cleared.");
	}

}
