# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Build args for CI/CD
ARG JWT_SECRET
ARG MAIL_HOST
ARG MAIL_PORT
ARG MAIL_USERNAME
ARG MAIL_PASSWORD

COPY pom.xml .
COPY src ./src

RUN mvn clean package -Dmaven.test.skip=true -Pdocker \
    -DJWT_SECRET="${JWT_SECRET}" \
    -DMAIL_HOST="${MAIL_HOST}" \
    -DMAIL_PORT="${MAIL_PORT}" \
    -DMAIL_USERNAME="${MAIL_USERNAME}" \
    -DMAIL_PASSWORD="${MAIL_PASSWORD}"

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup

# Copy built jar
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]