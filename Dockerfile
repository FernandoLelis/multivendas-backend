# =============================================
# MULTIVENDAS BACKEND - DOCKERFILE PARA PRODUÇÃO
# =============================================

# 1️⃣ FASE DE CONSTRUÇÃO
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Configurar diretório de trabalho
WORKDIR /app

# Copiar arquivos de configuração do Maven
COPY pom.xml .

# Baixar dependências (cache layer)
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Compilar aplicação
RUN mvn clean package -DskipTests -Pprod

# 2️⃣ FASE DE EXECUÇÃO
FROM eclipse-temurin:17-jre-alpine

# Instalar dependências do PostgreSQL
RUN apk add --no-cache bash tzdata

# Configurar fuso horário (Brasil)
ENV TZ=America/Sao_Paulo

# Criar usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Configurar diretório de trabalho
WORKDIR /app

# Copiar JAR da fase de construção
COPY --from=build /app/target/*.jar app.jar

# Expor porta
EXPOSE 8080

# Variáveis de ambiente padrão
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/auth/health || exit 1

# Comando de execução
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]