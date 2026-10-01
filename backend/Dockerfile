# Backend image. Build from the repo root (it needs shared/ and the Gradle files):
#   docker build -f backend/Dockerfile -t glide-backend .

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# No Android SDK in this image: tell settings.gradle.kts to skip the Android app.
ENV GLIDE_BACKEND_ONLY=true
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY shared ./shared
COPY backend ./backend
RUN ./gradlew :backend:installDist --no-daemon --quiet

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S glide && adduser -S glide -G glide
WORKDIR /app
COPY --from=build /src/backend/build/install/backend/ ./
# Never run as root inside the container.
USER glide
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT:-8080}/health/live" > /dev/null || exit 1
ENTRYPOINT ["./bin/backend"]
