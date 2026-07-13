# ---------- Build stage ----------
FROM maven:3.9.6-eclipse-temurin-25 AS build


WORKDIR /app

# Copy pom first to leverage Docker cache
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source
COPY src ./src

# Build the application
RUN mvn -B clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:25-jre

# Create non-root user
RUN groupadd -r app && useradd -r -g app app
USER app


WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (documentational)
EXPOSE 8080

# JVM + Spring optimizations for containers
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
