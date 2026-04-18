# ===========================
# 1단계: 빌드된 JAR에서 레이어 추출
# ===========================
FROM eclipse-temurin:21-jre AS builder

WORKDIR /builder

# GitHub Actions에서 준비한 실행 JAR 복사
COPY build/libs/app.jar app.jar

# Spring Boot layertools를 이용해 레이어를 추출
RUN java -Djarmode=layertools -jar app.jar extract

# ==================
# 2단계: 실행 전용 이미지
# ==================
FROM eclipse-temurin:21-jre

WORKDIR /app

# 비특권 사용자와 그룹 생성
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# 추출된 레이어를 순서대로 복사
COPY --from=builder /builder/dependencies/ ./
COPY --from=builder /builder/spring-boot-loader/ ./
COPY --from=builder /builder/snapshot-dependencies/ ./
COPY --from=builder /builder/application/ ./

# 파일 권한 부여
RUN chown -R appuser:appgroup /app

# 비특권 사용자로 실행
USER appuser

EXPOSE 8080

# Spring Boot loader를 직접 실행
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
