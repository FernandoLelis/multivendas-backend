# Use uma imagem com Java 17
FROM openjdk:17-jdk-slim

# Diretório de trabalho
WORKDIR /app

# Copie o arquivo JAR
COPY target/erp-vendas-0.0.1-SNAPSHOT.jar app.jar

# Porta que a aplicação usa
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]