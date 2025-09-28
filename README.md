# BrokerX - Plateforme de Courtage Sécurisée

[![CI Pipeline](https://github.com/monicaDelao/brokerx/actions/workflows/ci.yml/badge.svg)](https://github.com/monicaDelao/brokerx/actions/workflows/ci.yml)
[![Quality Gate](https://img.shields.io/badge/Quality%20Gate-Passing-brightgreen)](#)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue)](#)
[![Java](https://img.shields.io/badge/Java-21-orange)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)](#)

Application web monolithique Spring Boot pour la gestion sécurisée des clients et ordres de courtage financier avec authentification multi-facteurs (MFA) obligatoire.

**Projet** : Fondations architecturales LOG320 - Phase 1  
**Architecture** : Monolithique évolutive vers Microservices  
**Sécurité** : MFA obligatoire, audit complet, protection brute force

## Cas d'utilisation implémentés

- **UC-01** [COMPLET] : Inscription de client avec vérification d'identité (email/SMS)
- **UC-02** [COMPLET] : Authentification sécurisée avec MFA obligatoire  
- **UC-05** [PRÉPARÉ] : Placement d'ordres avec contrôles pré-trade (architecture préparée)

## Démarrage rapide (Docker - RECOMMANDÉ)

### Prérequis
- Docker Desktop installé
- Port 8081 disponible

### Lancement de l'application
```bash
# 1. Cloner le projet
git clone https://github.com/monicaDelao/brokerx.git
cd brokerx

# 2. Démarrer avec Docker Compose
docker compose up --build -d

# 3. Vérifier l'état
docker compose ps
```

### Accès à l'application
- **Application web** : http://localhost:8081
- **Health check** : http://localhost:8081/actuator/health  
- **Console H2** : http://localhost:8081/h2-console
  - URL: `jdbc:h2:file:/app/data/brokerx`
  - User: `brokerx` / Password: `brokerx`

## Structure du projet

```
src/
├── main/
│   ├── java/brokerx/
│   │   ├── BrokerXWebApplication.java  # Application Spring Boot
│   │   ├── controllers/
│   │   │   └── WebController.java      # Contrôleur web principal
│   │   ├── entity/                     # Entités JPA
│   │   │   ├── Client.java
│   │   │   ├── MfaConfig.java
│   │   │   ├── MfaOtpCode.java
│   │   │   └── TentativeConnexion.java
│   │   ├── repository/                 # Repositories JPA
│   │   │   ├── ClientRepository.java
│   │   │   ├── MfaConfigRepository.java
│   │   │   ├── MfaOtpCodeRepository.java
│   │   │   └── TentativeConnexionRepository.java
│   │   └── services/                   # Services métier
│   │       ├── ClientService.java
│   │       ├── MfaService.java
│   │       └── SecurityService.java
│   └── resources/
│       ├── templates/                  # Templates Thymeleaf
│       │   ├── index.html
│       │   ├── connexion.html
│       │   ├── inscription.html
│       │   ├── mfa-verification.html
│       │   └── inscription-confirmation.html
│       └── application.properties
└── docs/                              # Documentation complète
    ├── adr/                          # Architecture Decision Records
    ├── arc42/                        # Documentation Architecture
    ├── business-analysis/            # Analyse métier
    └── views/                        # Diagrammes UML
```

## Compilation et exécution

## Commandes Docker

### Commandes principales
```bash
# Démarrer l'application (build + run)
docker compose up --build -d

# Arrêter l'application
docker compose down

# Arrêter et nettoyer complètement
docker compose down --volumes --remove-orphans

# Voir les logs en temps réel
docker compose logs -f brokerx-web

# Vérifier l'état des conteneurs
docker compose ps

# Rebuilder sans cache
docker compose build --no-cache
```

### Gestion des volumes
```bash
# Voir les volumes créés
docker volume ls | findstr brokerx

# Nettoyer les volumes (ATTENTION: perte des données)
docker compose down --volumes
```

## Développement local (alternatif)

### Prérequis pour développement local
- Java 21+ (JDK 21)
- Gradle 8.5+ (inclus via wrapper)

### Commandes Gradle
```bash
# Démarrer l'application Spring Boot
./gradlew bootRun

# Compiler le projet
./gradlew build

# Créer un JAR exécutable
./gradlew bootJar
```

## Architecture technique

### Stack technologique
- **Spring Boot 3.2.0** : Framework principal
- **Spring Data JPA** : Persistance des données
- **Spring Boot Actuator** : Monitoring et health checks
- **H2 Database** : Base de données embarquée
- **Thymeleaf** : Moteur de templates
- **Bootstrap 5** : Framework CSS

### Architecture en couches
```
Presentation Layer    → WebController, Templates Thymeleaf
Application Services  → ClientService, MfaService, SecurityService  
Domain Layer         → Entités métier (Client, MfaConfig, etc.)
Data Access Layer    → Spring Data Repositories, JPA Entities
```

### Sécurité implémentée
- **MFA obligatoire** : Authentification à deux facteurs
- **Vérification email/SMS** : Validation des coordonnées
- **Audit des connexions** : Traçabilité complète des tentatives
- **Protection brute force** : Limitations par IP et délais
- **Hashage sécurisé** : Protection des mots de passe

## Documentation

### Architecture Decision Records (ADR)
- `docs/adr/adr.md` : Décisions architecturales documentées

### Documentation Arc42
- `docs/arc42/docs.md` : Documentation architecture complète

### Diagrammes UML
- `docs/views/` : Diagrammes PlantUML (cas d'utilisation, composants, déploiement)
- Exportés en PNG via `export-diagrams.ps1`

## Tests et validation

### Tests unitaires
- `src/main/java/brokerx/test/CU01Test.java` : Tests complets UC-01

### Endpoints de monitoring  
- `/actuator/health` : Statut de l'application
- `/actuator/info` : Informations sur l'application
- `/actuator/metrics` : Métriques de performance

## Critères d'acceptation Phase 1

### Livrables validés
- [x] Architecture monolithique évolutive (Spring Boot + couches)
- [x] UC-01 et UC-02 implémentés et testés
- [x] Persistance robuste (H2 + JPA)
- [x] Documentation complète (Arc42, ADR, diagrammes)
- [x] Conteneurisation Docker avec healthcheck
- [x] Tests automatisés et validation

### Définition de fini (DoD)
- [x] UC Must implémentés de bout en bout
- [x] App & DB conteneurisées et déployables
- [x] Documentation cohérente (4+1 views, Arc42, ADR)
- [x] Logs structurés et guide d'exploitation