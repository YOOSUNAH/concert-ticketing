FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :admin-api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
COPY --from=builder /workspace/admin-api/build/libs/admin-api.jar /app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app.jar"]
