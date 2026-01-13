# Stage 1: Build stage
FROM maven:3.9.5-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app
# Copy pom.xml and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Copy source code
COPY src ./src
# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
# Set working directory
WORKDIR /app
# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar
# Expose application port
EXPOSE 8081
# Run the application
ENTRYPOINT ["java", \
    "-jar", \
    "app.jar"]