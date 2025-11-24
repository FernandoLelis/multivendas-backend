# Use uma imagem com Maven e JDK já instalados (imagem válida)
FROM maven:3.8.7-openjdk-17 AS build

WORKDIR /app

# Copiar arquivos do projeto
COPY . .

# Fazer o build
RUN mvn clean package -DskipTests

# Imagem final menor
FROM openjdk:17-jre-slim

WORKDIR /app

# Copiar o JAR do estágio de build
COPY --from=build /app/target/*.jar app.jar

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Comando de execução
ENTRYPOINT ["java", "-jar", "app.jar"]