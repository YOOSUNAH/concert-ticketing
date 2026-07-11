FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :queue-api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
COPY --from=builder /workspace/queue-api/build/libs/queue-api.jar /app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
