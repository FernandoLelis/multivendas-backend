# Build stage
FROM eclipse-temurin:17-jdk as builder

WORKDIR /app

# ✅ CORREÇÃO: Copiar TODOS os arquivos do Maven Wrapper PRIMEIRO
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# ✅ CORREÇÃO: Dar permissão de execução ao mvnw (Linux/Mac)
RUN chmod +x mvnw

# Copiar código fonte
COPY src ./src

# ✅ CORREÇÃO: Usar o Maven Wrapper para build
RUN ./mvnw clean package -DskipTests

# Runtime stage - use JRE menor
FROM eclipse-temurin:17-jre

WORKDIR /app

# ✅ CORREÇÃO: Copiar o JAR gerado do stage de build
COPY --from=builder /app/target/*.jar app.jar

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Comando de execução
ENTRYPOINT ["java", "-jar", "app.jar"]
