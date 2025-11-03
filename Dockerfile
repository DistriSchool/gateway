# Build stage
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy only pom first to leverage Docker cache
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Add a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy fat jar
COPY --from=build /app/target/*.jar /app/app.jar

# Expose app port (matches server.port)
EXPOSE 8080 

USER spring

# Health-friendly startup options; allow overriding with JAVA_OPTS
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

