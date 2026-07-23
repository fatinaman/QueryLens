# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp clean package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S querylens && adduser -S querylens -G querylens
WORKDIR /app

COPY --from=build --chown=querylens:querylens \
  /workspace/target/querylens-backend-0.0.1-SNAPSHOT.jar \
  /app/querylens-backend.jar

USER querylens
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -jar /app/querylens-backend.jar"]
