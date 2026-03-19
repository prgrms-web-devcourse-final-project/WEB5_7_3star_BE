package com.threestar.trainus.domain.test.service;

import com.threestar.trainus.domain.lesson.issue.LessonApplyStreamConstant;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;

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

	@Transactional
	public void createUsers(int count) {
		log.info("Starting bulk user creation: {} users", count);

		// 유저 대량 생성 (ON CONFLICT로 중복 무시)
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

		// Redis 세션 워밍업 (세션 주입)
		warmUpSessions(count);

		log.info("Bulk creation completed: Users, Profiles, Metadata, and Redis Sessions are ready.");
	}

	private void warmUpSessions(int count) {
		log.info("Warming up {} sessions in Redis...", count);
		// 생성된 id 리스트를 가져와서 세션 주입
		jdbcTemplate.query("SELECT id FROM users WHERE email LIKE 'testuser%@example.com'", rs -> {
			String userId = String.valueOf(rs.getLong("id"));
			// 세션 키 형식: "test:session:{num}" -> value: userId
			stringRedisTemplate.opsForValue().set("test:session:" + userId, userId, Duration.ofHours(24));
		});
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

		// DB 초기화
		jdbcTemplate.execute(
			"TRUNCATE TABLE lesson_participants, lesson_applications, lesson_images, lessons, profile, profile_metadata, users RESTART IDENTITY CASCADE");

		// Redis 초기화 (Stream 비우기 및 관련 키 삭제)
		stringRedisTemplate.opsForStream().trim(LessonApplyStreamConstant.STREAM_KEY, 0);

		// 레슨 재고, 상태값, 테스트 세션 키 패턴 삭제
		java.util.Set<String> keys = stringRedisTemplate.keys("lesson:*");
		if (keys != null && !keys.isEmpty())
			stringRedisTemplate.delete(keys);

		java.util.Set<String> sessionKeys = stringRedisTemplate.keys("test:session:*");
		if (sessionKeys != null && !sessionKeys.isEmpty())
			stringRedisTemplate.delete(sessionKeys);

		log.info("Hard reset completed successfully.");
	}

}
