FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/macro-tracker-cloud-gateway-1.0.0.jar macro-tracker-cloud-gateway.jar
COPY opentelemetry-javaagent.jar /opt/opentelemetry/opentelemetry-javaagent.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "macro-tracker-cloud-gateway.jar"]