package com.threestar.trainus.testsupport;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.global.config.security.JwtAuthenticationFilter;
import com.threestar.trainus.global.config.security.JwtProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public abstract class ManualAwsIntegrationTestSupport {

	/*
	 * 실제 AWS 연동을 확인하는 수동 통합 테스트의 공통 실행 기반이다.
	 * PostgreSQL/PostGIS와 Redis는 Testcontainer로 띄우고, AWS 자격 증명과 S3 버킷 이름은 환경변수에서 읽는다.
	 */
	private static final DockerImageName POSTGIS_IMAGE = DockerImageName.parse("postgis/postgis:16-3.4")
		.asCompatibleSubstituteFor("postgres");
	private static final AtomicBoolean POSTGIS_INITIALIZED = new AtomicBoolean(false);

	@Container
	protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE)
		.withDatabaseName("trainus_it")
		.withUsername("trainus")
		.withPassword("trainus");

	@DynamicPropertySource
	static void registerIntegrationProperties(DynamicPropertyRegistry registry) {
		// PostgreSQL 연결 정보 주입
		registry.add("spring.datasource.url", ManualAwsIntegrationTestSupport::postgresJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
		registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");

		// Redis 연결 정보 주입
		registry.add("spring.data.redis.core.host", () -> "localhost");
		registry.add("spring.data.redis.core.port", () -> 6379);
		registry.add("spring.data.redis.mq.host", () -> "localhost");
		registry.add("spring.data.redis.mq.port", () -> 6379);
		registry.add("spring.data.redis.host", () -> "localhost");
		registry.add("spring.data.redis.port", () -> 6379);
		registry.add("spring.data.redis.key.prefix", () -> "redis://");

		// 공통 테스트용 메일 설정
		registry.add("spring.mail.username", () -> "test@example.com");
		registry.add("spring.mail.password", () -> "test-password");

		// 실제 AWS 계정 정보를 환경변수에서 읽는다.
		registry.add("cloud.aws.credentials.access-key", ManualAwsIntegrationTestSupport::requireEnvAwsAccessKey);
		registry.add("cloud.aws.credentials.secret-key", ManualAwsIntegrationTestSupport::requireEnvAwsSecretKey);
		registry.add("cloud.aws.s3.bucket", ManualAwsIntegrationTestSupport::requireEnvS3BucketName);
		registry.add("cloud.aws.region.static", () -> "ap-northeast-2");
		registry.add("cloud.aws.stack.auto", () -> "false");

		// 결제와 JWT 테스트에 필요한 더미 설정
		registry.add("payment.secret-key", () -> "test-payment-secret");
		registry.add("jwt.secret", () -> "test-jwt-secret-key-for-integration-tests-1234567890");
	}

	// Bearer 토큰을 만든다.
	protected String bearerToken(User user, JwtProvider jwtProvider) {
		return JwtAuthenticationFilter.BEARER_PREFIX + jwtProvider.createAccessToken(user.getId(), user.getRole().name());
	}

	private static String postgresJdbcUrl() {
		// 컨테이너 시작 후 최초 1회만 PostGIS 확장을 생성한다.
		POSTGRES.start();
		if (POSTGIS_INITIALIZED.compareAndSet(false, true)) {
			createPostgisExtension();
		}
		return POSTGRES.getJdbcUrl();
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

	// 환경변수가 비어 있으면 즉시 실패시킨다.
	private static String requireEnvAwsAccessKey() {
		return requireEnv("AWS_ACCESS_KEY");
	}

	// 환경변수가 비어 있으면 즉시 실패시킨다.
	private static String requireEnvAwsSecretKey() {
		return requireEnv("AWS_SECRET_KEY");
	}

	// 환경변수가 비어 있으면 즉시 실패시킨다.
	private static String requireEnvS3BucketName() {
		return requireEnv("S3_BUCKET_NAME");
	}

	// 지정한 환경변수 값을 읽는다.
	private static String requireEnv(String key) {
		String value = System.getenv(key);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(key + " 환경변수가 필요하다.");
		}
		return value;
	}
}
