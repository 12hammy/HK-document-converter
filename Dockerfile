# --- Build Stage ---
FROM maven:3.8.8-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# --- Run Stage ---
FROM eclipse-temurin:21-jdk
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
