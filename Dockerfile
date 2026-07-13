# --- Build Stage ---
FROM maven:3.8.8-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# --- Run Stage ---
FROM eclipse-temurin:21-jdk

# ONGEZEO: Sakinisha Tesseract OCR na mifumo ya Fonts ndani ya Java 21 ya Linux (Render)
RUN apt-get update && apt-get install -y \
    fontconfig \
    fonts-dejavu \
    tesseract-ocr \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
