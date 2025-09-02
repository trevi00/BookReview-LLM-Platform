# Multi-stage build for Spring Boot application

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle wrapper와 빌드 스크립트 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐시 최적화)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 필요한 패키지 설치
RUN apk add --no-cache curl

# 애플리케이션 사용자 생성
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 로그 및 업로드 디렉토리 생성
RUN mkdir -p /app/logs /app/uploads && \
    chown -R spring:spring /app

# 사용자 변경
USER spring:spring

# 포트 노출
EXPOSE 8080

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=30s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 옵션 설정
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]