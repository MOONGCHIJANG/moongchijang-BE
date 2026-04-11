# =================
# 애플리케이션 빌드 단계
# =================
FROM gradle:8.14.3-jdk21 AS builder

# 빌드 작업 디렉터리 설정
WORKDIR /app

# Gradle 실행 파일 복사
COPY gradlew .

# Gradle 설정 디렉터리 복사
COPY gradle gradle

# Gradle 빌드 설정 파일 복사
COPY build.gradle.kts .
COPY settings.gradle.kts .

# 애플리케이션 소스 복사
COPY src src

# Gradle 실행 권한 부여
RUN chmod +x ./gradlew

# Spring Boot 실행 JAR 생성
RUN ./gradlew bootJar --no-daemon

# =================
# 애플리케이션 실행 단계
# =================
FROM eclipse-temurin:21-jre

# 실행 작업 디렉터리 설정
WORKDIR /app

# 실행 가능한 JAR 복사
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# 애플리케이션 포트 노출
EXPOSE 8080

# 애플리케이션 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]
