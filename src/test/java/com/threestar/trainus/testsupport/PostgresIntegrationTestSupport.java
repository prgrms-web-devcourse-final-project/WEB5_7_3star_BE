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

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public abstract class PostgresIntegrationTestSupport {

	/*
	 * PostgreSQL/PostGIS가 필요한 통합 테스트의 공통 실행 기반이다.
	 * PostgreSQL Testcontainer를 띄우고 Spring datasource, JWT, 외부 연동용 더미 설정을 테스트 환경에 주입한다.
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

	@DynamicPropertySource
	static void registerIntegrationProperties(DynamicPropertyRegistry registry) {
		// PostgreSQL 연결 정보 주입
		registry.add("spring.datasource.url", PostgresIntegrationTestSupport::postgresJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
		registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");

		// Redis와 외부 연동용 더미 설정 주입
		registry.add("spring.data.redis.core.host", () -> "localhost");
		registry.add("spring.data.redis.core.port", () -> 6379);
		registry.add("spring.data.redis.mq.host", () -> "localhost");
		registry.add("spring.data.redis.mq.port", () -> 6379);
		registry.add("spring.data.redis.host", () -> "localhost");
		registry.add("spring.data.redis.port", () -> 6379);
		registry.add("spring.data.redis.key.prefix", () -> "redis://");

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
}
