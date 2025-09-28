# ADR 001 – Architecture Monolithique Spring Boot avec Séparation en Couches

## Statut
Acceptée

## Contexte

L'application BrokerX doit implémenter les cas d'utilisation UC-01 (inscription avec MFA), UC-02 (authentification sécurisée) et UC-05 (placement d'ordres avec contrôles pré-trade). Le système doit être conçu avec des fondations architecturales solides, évolutives et maintenables, permettant une future transition vers une architecture distribuée si nécessaire.

## Décision

Nous adoptons une architecture **monolithique Spring Boot** organisée en **couches distinctes** avec séparation claire des responsabilités :

* **Presentation Layer (WebController)** : Gère les requêtes HTTP, la validation des formulaires et l'orchestration des appels vers la couche service. Utilise Thymeleaf pour le rendu des vues.

* **Application Services Layer** : Contient la logique applicative avec des services métier spécialisés :
  - `ClientService` : Orchestration de l'inscription et gestion des clients (UC-01)
  - `MfaService` : Génération/validation des codes OTP pour l'authentification multi-facteurs (UC-02)
  - `SecurityService` : Gestion de la sécurité, audit des connexions et protection contre le brute force
  - `OrdreService` : Logique de placement d'ordres et contrôles pré-trade (UC-05)

* **Domain Layer** : Représente le cœur métier avec les entités du domaine (`Client`, `Ordre`, `CompteDeCourtage`, `MfaConfig`, etc.) et les règles métier encapsulées.

* **Data Access Layer** : Utilise Spring Data JPA avec des repositories (`ClientRepository`, `MfaConfigRepository`, etc.) pour abstraire la persistance et permettre l'interchangeabilité des bases de données.

## Conséquences

### Avantages
- **Séparation des responsabilités** : Chaque couche a un rôle bien défini
- **Testabilité** : Les couches peuvent être testées indépendamment via injection de dépendances
- **Évolutivité** : Architecture préparée pour une future décomposition en microservices
- **Maintenabilité** : Code organisé selon les bonnes pratiques Spring Boot
- **Interchangeabilité** : La couche d'accès aux données permet de changer de SGBD (actuellement H2, facilement remplaçable par PostgreSQL, MySQL, etc.)

### Inconvénients
- **Complexité initiale** : Plus d'abstractions que nécessaire pour une application simple
- **Performance** : Monolithe unique point de charge (acceptable pour la phase initiale)

Cette architecture pose des fondations solides pour BrokerX tout en respectant les principes de conception étudiés en cours et en préparant l'évolution future du système.

---

# ADR 002 – Authentification Multi-Facteurs (MFA) Obligatoire

## Statut
Acceptée

## Contexte

L'application BrokerX gère des données financières sensibles et des transactions d'ordres. La sécurité est critique pour protéger les comptes clients et prévenir les accès non autorisés. Une authentification par simple mot de passe est insuffisante pour ce niveau de sécurité requis.

## Décision

Nous implémentons un système d'**authentification multi-facteurs (MFA) obligatoire** avec les composants suivants :

* **Configuration MFA par client** : Entité `MfaConfig` permettant d'activer/désactiver MFA et configurer les méthodes disponibles
* **Support multi-méthodes** : SMS, TOTP (Google Authenticator), codes de backup
* **Génération de codes OTP** : Entité `MfaOtpCode` avec expiration temporelle (10 minutes)
* **Audit complet** : Traçabilité via `TentativeConnexion` pour toutes les tentatives d'authentification
* **Protection contre le brute force** : Limitation par IP et nombre de tentatives

## Conséquences

### Avantages
- **Sécurité renforcée** : Protection contre les attaques par dictionnaire et credential stuffing
- **Conformité** : Respect des standards de sécurité pour les applications financières
- **Flexibilité** : Multiple méthodes MFA selon les préférences utilisateur
- **Auditabilité** : Traçabilité complète des connexions pour investigations

### Inconvénients
- **Complexité utilisateur** : Étape supplémentaire lors de la connexion
- **Dépendance externe** : Simulation d'envoi SMS (à remplacer par un vrai service en production)

Cette décision garantit un niveau de sécurité approprié pour une application de courtage financier.

---

# ADR 003 – Base de Données H2 Embarquée pour le Développement

## Statut
Acceptée

## Contexte

Pour la phase de développement et de démonstration, nous avons besoin d'une solution de base de données simple à configurer, sans dépendances externes, tout en utilisant un modèle relationnel standard compatible avec les bases de données de production.

## Décision

Nous utilisons **H2 Database** en mode fichier embarqué avec les caractéristiques suivantes :

* **Mode fichier** : Persistance dans `./data/brokerx` pour conservation des données entre redémarrages
* **Console H2** : Interface web activée sur `/h2-console` pour inspection et debugging
* **Hibernate DDL** : Mode `update` pour création/mise à jour automatique du schéma
* **Compatibilité SQL** : Dialecte standard permettant migration future vers PostgreSQL/MySQL

## Conséquences

### Avantages
- **Simplicité de déploiement** : Aucune configuration externe requise
- **Rapidité de développement** : Base intégrée au JAR Spring Boot
- **Console de debugging** : Interface graphique pour inspection des données
- **Migration facile** : Changement de `spring.datasource.url` suffit pour passer en production

### Inconvénients
- **Limitations de performance** : Non adaptée pour la charge de production
- **Concurrence limitée** : Accès mono-utilisateur en mode fichier
- **Pas de clustering** : Solution non distribuée

Cette solution est idéale pour les fondations architecturales et sera remplacée par PostgreSQL en production.

---

# ADR 004 – Conteneurisation Docker avec Déploiement Docker Compose

## Statut
Acceptée

## Contexte

L'application BrokerX doit être déployée de manière standardisée et reproductible pour faciliter le développement, les tests et la démonstration. Les exigences du projet incluent une conteneurisation complète avec déploiement via Docker Compose, incluant l'application, la base de données et les vérifications de santé.

## Décision

Nous implémentons une **architecture conteneurisée complète** basée sur Docker avec les composants suivants :

* **Dockerfile multi-stage** : Construction optimisée avec Eclipse Temurin 21 (JDK pour le build, JRE Alpine pour l'exécution)
* **Docker Compose** : Orchestration complète avec volumes persistants, healthcheck et configuration d'environnement
* **Volumes persistants** : `brokerx_data` pour la base H2 et `brokerx_logs` pour les logs applicatifs
* **Healthcheck intégré** : Vérification via Spring Boot Actuator (`/actuator/health`)
* **Configuration par profil** : `application-docker.properties` pour optimiser l'exécution conteneurisée
* **Isolation réseau** : Réseau Docker dédié pour la sécurité et l'isolation

## Conséquences

### Avantages
- **Reproductibilité** : Environnement identique sur tous les postes de développement
- **Isolation** : Application conteneurisée sans pollution de l'environnement hôte
- **Facilité de déploiement** : `docker compose up --build -d` lance l'environnement complet
- **Monitoring intégré** : Healthcheck automatique pour vérifier l'état de l'application
- **Persistance des données** : Volumes Docker garantissent la conservation des données
- **Optimisation des ressources** : Image multi-stage réduit la taille de l'image finale

### Inconvénients
- **Dépendance Docker** : Nécessite Docker et Docker Compose sur l'environnement de développement
- **Complexité initiale** : Configuration supplémentaire par rapport à l'exécution directe
- **Debugging** : Accès aux logs via `docker compose logs` plutôt que directement en console

Cette architecture conteneurisée respecte les standards modernes de déploiement et facilite la transition vers des environnements de production orchestrés (Kubernetes, Docker Swarm).