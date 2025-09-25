# BrokerX

Une application Java pour la gestion de clients utilisant l'architecture MVC.

## Prérequis

- Java 21 (JDK 21) - Dernière version LTS
- Gradle 8.5+ (inclus via le wrapper)

## Structure du projet

```
src/
├── main/java/brokerx/
│   ├── BrokerManager.java      # Classe principale
│   ├── controllers/
│   │   └── ClientController.java
│   ├── models/
│   │   └── Client.java
│   ├── daos/
│   │   └── ClientDAO.java
│   └── views/
│       └── ConsoleView.java
└── test/
    └── (tests unitaires)
```

## Compilation et exécution

### Avec Gradle (recommandé)

```bash
# Compiler le projet
./gradlew build

# Exécuter l'application
./gradlew run

# Créer un JAR exécutable
./gradlew fatJar
java -jar build/libs/brokerx-1.0.0-all.jar
```

### Avec Docker

```bash
# Construire l'image Docker
docker build -f src/dockerfile -t brokerx:latest .

# Exécuter le conteneur
docker run -it brokerx:latest
```

## Mise à niveau vers Java 21

Ce projet a été mis à niveau vers Java 21 (LTS) depuis Java 11. Les changements incluent :

- **build.gradle** : Mise à jour de `sourceCompatibility` et `targetCompatibility` vers `VERSION_21`
- **Dockerfile** : Utilisation d'images basées sur OpenJDK 21
- **Structure des packages** : Réorganisation selon les standards Maven/Gradle
- **Build amélioré** : Ajout du support pour les fat JARs

## Fonctionnalités

- **CU01** : Créer un client
- **CU02** : Lister tous les clients
- Interface console interactive

## Architecture

- **MVC Pattern** : Séparation claire entre Modèles, Vues et Contrôleurs
- **DAO Pattern** : Accès aux données via des objets d'accès dédiés
- **Packaging Java standard** : Respect des conventions Maven/Gradle