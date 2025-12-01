FROM olehprukhnytskyi/base-java-otel:21
WORKDIR /app
COPY target/macro-tracker-cloud-gateway-1.0.0.jar macro-tracker-cloud-gateway.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "macro-tracker-cloud-gateway.jar"]