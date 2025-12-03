# Use Amazon Corretto 17
FROM amazoncorretto:17-alpine-jdk

# Diretório de trabalho
WORKDIR /app

# Copie o pom.xml e baixe dependências primeiro (cache eficiente)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copie o código fonte
COPY src ./src

# Compile o projeto e gere o JAR
RUN mvn clean package -DskipTests

# Copie o JAR gerado (nome correto)
COPY target/erp-vendas-*.jar app.jar

# Porta que a aplicação usa
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]