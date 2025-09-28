# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Installer Gradle directement
RUN apk add --no-cache gradle

# Copier tous les fichiers sources
COPY . .

# Build l'application avec Gradle natif
RUN gradle build --no-daemon --stacktrace

# Runtime stage pour BrokerX Web Interface
FROM eclipse-temurin:21-jre-alpine

# Informations de l'image
LABEL maintainer="BrokerX Team"
LABEL description="BrokerX Web Interface"
LABEL version="1.0.0"

WORKDIR /app

# Copier l'application JAR depuis le stage de build
COPY --from=build /app/build/libs/*.jar app.jar

# Variables d'environnement par défaut
ENV JAVA_OPTS="-Xmx256m -Xms128m" \
    LOG_LEVEL="INFO" \
    SERVER_PORT="8081"

# Créer répertoire pour les données H2
RUN mkdir -p /app/data

# Port d'écoute pour l'interface web (Spring Boot default 8081)
EXPOSE 8081

# Healthcheck intégré Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Point d'entrée pour l'application web
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$SERVER_PORT -jar app.jar"]