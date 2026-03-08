# --- Build stage: GraalVM native-image ---
FROM ghcr.io/graalvm/native-image-community:17 AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src src

RUN chmod +x gradlew && ./gradlew nativeCompile --no-daemon -x test

# --- Runtime stage: minimal image (~50 MB) ---
FROM debian:12-slim
WORKDIR /app

COPY --from=build /app/build/native/nativeCompile/otp-manager .

EXPOSE 8080
ENTRYPOINT ["./otp-manager"]
