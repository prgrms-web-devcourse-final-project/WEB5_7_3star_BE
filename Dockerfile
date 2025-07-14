# Build stage
FROM bellsoft/liberica-openjdk-alpine:21 AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle wrapper와 설정 파일들 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 (캐시 최적화)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# JAR 파일 생성
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM bellsoft/liberica-openjdk-alpine:21

# 작업 디렉토리 설정
WORKDIR /app

# 애플리케이션 사용자 생성
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 파일 권한 설정
RUN chown appuser:appgroup app.jar

# 사용자 전환
USER appuser

# 포트 노출
EXPOSE 8080

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]