# 애플리케이션 실행 전용 이미지
FROM eclipse-temurin:21-jre

# 애플리케이션 작업 디렉터리 설정
WORKDIR /app

# GitHub Actions에서 미리 빌드한 실행 JAR 복사
COPY build/libs/*-SNAPSHOT.jar app.jar

# 애플리케이션 포트 노출
EXPOSE 8080

# Spring Boot 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
