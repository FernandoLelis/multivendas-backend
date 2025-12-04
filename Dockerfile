# Dockerfile para Spring Boot com Maven
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build

# Define diretório de trabalho
WORKDIR /app

# Copia arquivos do Maven primeiro (cache de dependências)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código fonte
COPY src ./src

# Build da aplicação
RUN mvn clean package -DskipTests

# Segunda etapa: imagem final menor
FROM eclipse-temurin:17-jre-alpine

# Instala curl para health checks (opcional, pode remover se quiser)
RUN apk add --no-cache curl

# Define diretório de trabalho
WORKDIR /app

# Copia o JAR do estágio de build
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta (Render usa variável PORT)
EXPOSE 10000

# Comando para rodar a aplicação
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-10000}"]