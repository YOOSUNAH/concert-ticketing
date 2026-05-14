FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :concert-api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
COPY --from=builder /workspace/concert-api/build/libs/concert-api.jar /app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app.jar"]
