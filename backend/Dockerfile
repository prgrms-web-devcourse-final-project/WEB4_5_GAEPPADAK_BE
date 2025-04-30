# 1단계: 빌드
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Gradle 설정 파일 복사
COPY build.gradle .
COPY settings.gradle .

# 의존성 캐싱
RUN gradle dependencies --no-daemon || true

# 전체 프로젝트 복사
COPY . .

# 빌드
RUN gradle build --no-daemon

# plain JAR 제거
RUN rm -f /app/build/libs/*-plain.jar

# 2단계: 실행
FROM eclipse-temurin:21-jre

# Doppler CLI 설치
RUN (curl -Ls --tlsv1.2 --proto "=https" --retry 3 https://cli.doppler.com/install.sh || wget -t 3 -qO- https://cli.doppler.com/install.sh) | sh

WORKDIR /app

# 빌드 결과 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 앱 실행
ENTRYPOINT ["doppler", "run", "--"]

CMD ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]

