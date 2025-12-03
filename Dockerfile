# Use Amazon Corretto 17 (JDK da Amazon - sempre funciona)
FROM amazoncorretto:17-alpine-jdk

# Diretório de trabalho
WORKDIR /app

# Copie o arquivo JAR
COPY target/erp-vendas-0.0.1-SNAPSHOT.jar app.jar

# Porta que a aplicação usa
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]