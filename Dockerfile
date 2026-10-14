# Build stage: Maven runs inside Docker; no Maven installation required on EC2.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

# Runtime stage: Java 21 only, no build tool or source code.
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S spring -G spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/target/notification-service-*.jar /app/app.jar
USER spring
EXPOSE 8082
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
