FROM eclipse-temurin:17.0.10_7-jdk

WORKDIR /app

COPY target/monitoring-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
