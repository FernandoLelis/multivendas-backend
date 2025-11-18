# Use uma imagem oficial do OpenJDK (versão válida)
FROM openjdk:21-slim

# Diretório de trabalho
WORKDIR /app

# Copie o arquivo JAR da aplicação
COPY target/*.jar app.jar

# Exponha a porta que a aplicação roda
EXPOSE 8080

# Comando para executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
