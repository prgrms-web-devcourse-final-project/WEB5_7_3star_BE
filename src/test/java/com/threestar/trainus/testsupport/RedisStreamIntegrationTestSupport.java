package com.threestar.trainus.testsupport;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public abstract class RedisStreamIntegrationTestSupport {

	/*
	 * Redis Stream과 PostgreSQL/PostGIS가 함께 필요한 통합 테스트의 공통 실행 기반이다.
	 * Redis/PostgreSQL Testcontainer를 띄우고 Spring datasource/redis property를 테스트 컨테이너 주소로 주입한다.
	 */
	/*
	 * macOS Docker Desktop 29 환경에서는 Docker API 1.40 이상이 필요하다.
	 * Testcontainers/docker-java가 더 낮은 /v1.xx/info endpoint를 호출하면 Docker가 실행 중이어도 빈 daemon
	 * 응답 때문에 실패할 수 있다. build.gradle의 test task의 DOCKER_HOST / api.version 설정과 함께 유지해야 한다.
	 */
	private static final DockerImageName POSTGIS_IMAGE = DockerImageName.parse("postgis/postgis:16-3.4")
		.asCompatibleSubstituteFor("postgres");
	private static final AtomicBoolean POSTGIS_INITIALIZED = new AtomicBoolean(false);

	@Container
	protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE)
		.withDatabaseName("trainus_it")
		.withUsername("trainus")
		.withPassword("trainus");

	@Container
	protected static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
		.withExposedPorts(6379);

	@Autowired
	protected StringRedisTemplate coreRedisTemplate;

	@Autowired
	protected StringRedisTemplate mqRedisTemplate;

	@DynamicPropertySource
	static void registerIntegrationProperties(DynamicPropertyRegistry registry) {
		// PostgreSQL 연결 정보 주입
		registry.add("spring.datasource.url", RedisStreamIntegrationTestSupport::postgresJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
		registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");

		// Redis Core / Redis MQ 연결 정보 주입
		registry.add("spring.data.redis.core.host", REDIS::getHost);
		registry.add("spring.data.redis.core.port", RedisStreamIntegrationTestSupport::redisPort);
		registry.add("spring.data.redis.mq.host", REDIS::getHost);
		registry.add("spring.data.redis.mq.port", RedisStreamIntegrationTestSupport::redisPort);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", RedisStreamIntegrationTestSupport::redisPort);
		registry.add("spring.data.redis.key.prefix", () -> "redis://");

		// 공통 테스트용 외부 연동 더미 설정
		registry.add("spring.mail.username", () -> "test@example.com");
		registry.add("spring.mail.password", () -> "test-password");
		registry.add("cloud.aws.credentials.access-key", () -> "test-access-key");
		registry.add("cloud.aws.credentials.secret-key", () -> "test-secret-key");
		registry.add("cloud.aws.s3.bucket", () -> "test-bucket");
		registry.add("cloud.aws.region.static", () -> "ap-northeast-2");
		registry.add("cloud.aws.stack.auto", () -> "false");
		registry.add("payment.secret-key", () -> "test-payment-secret");
		registry.add("jwt.secret", () -> "test-jwt-secret-key-for-integration-tests-1234567890");
	}

	@AfterEach
	void clearRedis() {
		// 테스트 간 Redis 데이터가 섞이지 않도록 전체 키를 비운다.
		deleteKeys(coreRedisTemplate);
		deleteKeys(mqRedisTemplate);
	}

	private static void createPostgisExtension() {
		try (var connection = DriverManager.getConnection(
			POSTGRES.getJdbcUrl(),
			POSTGRES.getUsername(),
			POSTGRES.getPassword()
		);
			var statement = connection.createStatement()) {
			statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to initialize PostGIS extension for integration tests", e);
		}
	}

	private static String postgresJdbcUrl() {
		// 컨테이너 시작 후 최초 1회만 PostGIS 확장을 생성한다.
		POSTGRES.start();
		if (POSTGIS_INITIALIZED.compareAndSet(false, true)) {
			createPostgisExtension();
		}
		return POSTGRES.getJdbcUrl();
	}

	private static Integer redisPort() {
		// Redis 컨테이너 포트를 매핑해서 돌려준다.
		REDIS.start();
		return REDIS.getMappedPort(6379);
	}

	private static void deleteKeys(StringRedisTemplate redisTemplate) {
		// Redis의 모든 키를 삭제한다.
		Set<String> keys = redisTemplate.keys("*");
		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}
	}
}
