# =========================
# 1단계: Spring Boot 빌드
# =========================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

COPY src ./src

RUN ./gradlew clean bootJar --no-daemon

RUN cp "$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" app.jar


# =========================
# 2단계: Spring Boot 실행
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]