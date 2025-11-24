FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
COPY opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar macro-tracker-cloud-gateway.jar
COPY --from=build /app/opentelemetry-javaagent.jar /opt/opentelemetry/opentelemetry-javaagent.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "macro-tracker-cloud-gateway.jar"]