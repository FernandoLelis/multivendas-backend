# Build stage - usar JDK 17 (LTS)
FROM eclipse-temurin:17-jdk as builder

WORKDIR /app

# ✅ Copiar arquivos do Maven Wrapper PRIMEIRO
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# ✅ Dar permissão de execução ao mvnw
RUN chmod +x mvnw

# Copiar código fonte
COPY src ./src

# ✅ Usar Maven Wrapper para build
RUN ./mvnw clean package -DskipTests

# ✅ DEBUG: Listar arquivos para verificar o JAR
RUN ls -la /app/target/

# Runtime stage - use JRE 17 (menor)
FROM eclipse-temurin:17-jre

WORKDIR /app

# ✅ CORREÇÃO: Copiar JAR com o nome EXATO gerado pelo Spring Boot
COPY --from=builder /app/target/erp-vendas-0.0.1-SNAPSHOT.jar app.jar

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Comando de execução
ENTRYPOINT ["java", "-jar", "app.jar"]