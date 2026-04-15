# 애플리케이션 실행 전용 이미지
FROM eclipse-temurin:21-jre

# 애플리케이션 작업 디렉터리 설정
WORKDIR /app

# 비특권 사용자와 그룹 생성
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# GitHub Actions에서 준비한 실행 JAR 복사
COPY build/libs/app.jar app.jar

# 애플리케이션 실행 권한을 appuser에게 부여
RUN chown appuser:appgroup /app/app.jar

# 비특권 사용자로 애플리케이션 실행
USER appuser

# 애플리케이션 포트 노출
EXPOSE 8080

# Spring Boot 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
