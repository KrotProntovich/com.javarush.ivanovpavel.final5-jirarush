# Этап сборки
FROM maven:3.8.1-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src/ src/
RUN mvn dependency:resolve
RUN mvn clean install -P prod

# Этап запуска
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/target/jira-1.0.jar .
COPY src/main/resources/ resources/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "jira-1.0.jar"]
