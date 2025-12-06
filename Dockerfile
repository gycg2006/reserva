FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar arquivo pom.xml primeiro (para cache de dependências)
COPY pom.xml .

# Baixar dependências (cache layer)
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Compilar e gerar JAR
RUN mvn clean package -DskipTests

# Imagem final
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar JAR da etapa de build
COPY --from=build /app/target/*.jar app.jar

# Expor porta
EXPOSE 8080

# Executar aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]

