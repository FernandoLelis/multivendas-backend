FROM openjdk:17

WORKDIR /app

# Copiar arquivos do projeto
COPY . .

# Instalar Maven e fazer build
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Comando de execução
ENTRYPOINT ["java", "-jar", "target/erp-vendas-0.0.1-SNAPSHOT.jar"]