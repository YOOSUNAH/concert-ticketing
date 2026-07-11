FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :queue-worker:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
COPY --from=builder /workspace/queue-worker/build/libs/queue-worker.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
