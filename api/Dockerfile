FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
COPY --from=builder /workspace/api/build/libs/api.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
