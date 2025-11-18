# Use a imagem base mais básica do OpenJDK
FROM openjdk:17

WORKDIR /app

# Copie o JAR (precisamos construir primeiro)
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
