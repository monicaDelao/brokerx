# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copier les fichiers de configuration Gradle
COPY gradle/ gradle/
COPY build.gradle settings.gradle gradlew ./

# Copier le code source
COPY src/ src/

# Build l'application
RUN chmod +x gradlew && ./gradlew build --no-daemon

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
    LOG_LEVEL="INFO"

# Port d'écoute pour l'interface web
EXPOSE 8080

# Point d'entrée pour l'application web
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]