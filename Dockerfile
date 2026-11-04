# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Fuso de Brasília: sem isso o JRE do Alpine roda em UTC e "hoje" vira amanhã a partir das 21h
# (cards, régua de cobrança, crons). A JVM também recebe -Duser.timezone abaixo.
ENV TZ=America/Sao_Paulo
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD java -Duser.timezone=America/Sao_Paulo -Dserver.port=${PORT:-8080} -jar app.jar --spring.profiles.active=prod
