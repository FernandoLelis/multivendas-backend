# Use Amazon Corretto 17
FROM amazoncorretto:17-alpine-jdk

# Instale Maven (Alpine Linux usa apk)
RUN apk add --no-cache maven

# Diretório de trabalho
WORKDIR /app

# Copie o pom.xml primeiro para cache eficiente
COPY pom.xml .

# Baixe dependências
RUN mvn dependency:go-offline

# Copie o código fonte
COPY src ./src

# Compile o projeto
RUN mvn clean package -DskipTests

# Porta que a aplicação usa
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "target/erp-vendas-0.0.1-SNAPSHOT.jar"]