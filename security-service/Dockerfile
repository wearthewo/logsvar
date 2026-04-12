FROM eclipse-temurin:17.0.10_7-jdk

WORKDIR /app

COPY target/security-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
