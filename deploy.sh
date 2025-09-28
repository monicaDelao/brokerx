#!/bin/bash

# ========================================
# Script de déploiement BrokerX
# ========================================
# Usage: ./deploy.sh [dev|prod] [version]
# Exemples:
#   ./deploy.sh dev           # Déploie en mode développeur
#   ./deploy.sh prod v1.2.3   # Déploie version v1.2.3 en production
# ========================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="brokerx"
DEPLOY_USER="log430"
DEPLOY_HOST="10.194.32.238"
REMOTE_DIR="/home/log430/brokerx"

# Couleurs pour les logs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonctions utilitaires
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Fonction d'aide
show_help() {
    cat << EOF
Script de déploiement BrokerX

USAGE:
    $0 [ENVIRONMENT] [VERSION] [OPTIONS]

ENVIRONMENTS:
    dev     Déploiement en mode développement (H2 database)
    prod    Déploiement en mode production (MySQL database)

OPTIONS:
    --rollback      Rollback vers la version précédente
    --status        Affiche le statut des services
    --logs          Affiche les logs des conteneurs
    --help          Affiche cette aide

EXEMPLES:
    $0 dev                    # Déploie en mode développement
    $0 prod v1.2.3           # Déploie version v1.2.3 en production
    $0 prod --rollback       # Rollback en production
    $0 --status              # Statut des services
    $0 --logs                # Voir les logs

EOF
}

# Vérification des prérequis
check_prerequisites() {
    log_info "Vérification des prérequis..."
    
    # Vérifier ssh
    if ! command -v ssh &> /dev/null; then
        log_error "SSH n'est pas installé"
        exit 1
    fi
    
    # Vérifier la connectivité
    if ! ssh -o ConnectTimeout=5 -o BatchMode=yes ${DEPLOY_USER}@${DEPLOY_HOST} exit 2>/dev/null; then
        log_error "Impossible de se connecter à ${DEPLOY_HOST}"
        log_info "Assurez-vous que votre clé SSH est configurée"
        exit 1
    fi
    
    log_success "Prérequis validés"
}

# Build de l'application
build_application() {
    log_info "Build de l'application..."
    
    if [ -f "gradlew" ]; then
        ./gradlew clean build -x test --no-daemon
    elif [ -f "gradlew.bat" ]; then
        ./gradlew.bat clean build -x test --no-daemon
    else
        log_error "Impossible de trouver gradlew"
        exit 1
    fi
    
    log_success "Build terminé"
}

# Préparation des fichiers de déploiement
prepare_deployment() {
    local env=$1
    local version=${2:-"latest"}
    
    log_info "Préparation du déploiement pour l'environnement: $env"
    
    # Créer le dossier temporaire
    local temp_dir="/tmp/brokerx-deploy-$(date +%s)"
    mkdir -p "$temp_dir"
    
    # Copier les fichiers nécessaires
    cp docker-compose.yml "$temp_dir/"
    cp -r nginx "$temp_dir/" 2>/dev/null || true
    cp -r db-init "$temp_dir/" 2>/dev/null || true
    
    # Créer le fichier .env pour l'environnement
    if [ "$env" = "prod" ]; then
        cat > "$temp_dir/.env" << EOF
# Configuration Production
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/brokerx_db
SPRING_DATASOURCE_USERNAME=brokerx_user
SPRING_DATASOURCE_PASSWORD=brokerx_pass_2024!
SPRING_H2_CONSOLE_ENABLED=false
SPRING_JPA_HIBERNATE_DDL_AUTO=update

# MySQL Configuration
MYSQL_ROOT_PASSWORD=brokerx_root_2024!
MYSQL_DATABASE=brokerx_db
MYSQL_USER=brokerx_user
MYSQL_PASSWORD=brokerx_pass_2024!

# Version
BROKERX_VERSION=$version
EOF
    else
        cat > "$temp_dir/.env" << EOF
# Configuration Développement
SPRING_PROFILES_ACTIVE=docker
SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/brokerx;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
SPRING_DATASOURCE_USERNAME=brokerx
SPRING_DATASOURCE_PASSWORD=brokerx
SPRING_H2_CONSOLE_ENABLED=true
SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Version
BROKERX_VERSION=$version
EOF
    fi
    
    echo "$temp_dir"
}

# Déploiement sur le serveur
deploy_to_server() {
    local env=$1
    local version=${2:-"latest"}
    local temp_dir=$3
    
    log_info "Déploiement sur le serveur..."
    
    # Créer le répertoire de déploiement
    ssh ${DEPLOY_USER}@${DEPLOY_HOST} "mkdir -p ${REMOTE_DIR}"
    
    # Sauvegarder la version actuelle
    ssh ${DEPLOY_USER}@${DEPLOY_HOST} "
        if [ -d '${REMOTE_DIR}/current' ]; then
            rm -rf ${REMOTE_DIR}/backup 2>/dev/null || true
            mv ${REMOTE_DIR}/current ${REMOTE_DIR}/backup
        fi
    "
    
    # Copier les nouveaux fichiers
    scp -r "$temp_dir" ${DEPLOY_USER}@${DEPLOY_HOST}:${REMOTE_DIR}/current
    
    # Déployer selon l'environnement
    if [ "$env" = "prod" ]; then
        ssh ${DEPLOY_USER}@${DEPLOY_HOST} "
            cd ${REMOTE_DIR}/current &&
            docker-compose --profile production down --remove-orphans 2>/dev/null || true &&
            docker-compose --profile production pull &&
            docker-compose --profile production up -d
        "
    else
        ssh ${DEPLOY_USER}@${DEPLOY_HOST} "
            cd ${REMOTE_DIR}/current &&
            docker-compose down --remove-orphans 2>/dev/null || true &&
            docker-compose up -d brokerx-web
        "
    fi
    
    log_success "Déploiement terminé"
}

# Rollback vers la version précédente
rollback() {
    log_info "Rollback vers la version précédente..."
    
    ssh ${DEPLOY_USER}@${DEPLOY_HOST} "
        cd ${REMOTE_DIR} &&
        if [ -d 'backup' ]; then
            docker-compose -f current/docker-compose.yml down --remove-orphans 2>/dev/null || true
            rm -rf current
            mv backup current
            cd current
            docker-compose up -d
            echo 'Rollback effectué avec succès'
        else
            echo 'Aucune version de backup trouvée'
            exit 1
        fi
    "
    
    log_success "Rollback terminé"
}

# Statut des services
show_status() {
    log_info "Statut des services..."
    
    ssh ${DEPLOY_USER}@${DEPLOY_HOST} "
        cd ${REMOTE_DIR}/current 2>/dev/null || { echo 'Aucun déploiement trouvé'; exit 1; }
        echo '=== Statut des conteneurs ==='
        docker-compose ps
        echo
        echo '=== Health checks ==='
        docker-compose exec -T brokerx-web wget -qO- http://localhost:8081/actuator/health 2>/dev/null || echo 'Application non accessible'
    "
}

# Affichage des logs
show_logs() {
    local service=${1:-""}
    
    log_info "Logs des services..."
    
    if [ -n "$service" ]; then
        ssh ${DEPLOY_USER}@${DEPLOY_HOST} "
            cd ${REMOTE_DIR}/current 2>/dev/null || { echo 'Aucun déploiement trouvé'; exit 1; }
            docker-compose logs -f --tail=100 $service
        "
    else
        ssh ${DEPLOY_USER}@${DEPLOY_HOST} "
            cd ${REMOTE_DIR}/current 2>/dev/null || { echo 'Aucun déploiement trouvé'; exit 1; }
            docker-compose logs --tail=100
        "
    fi
}

# Main function
main() {
    local env=""
    local version="latest"
    local action="deploy"
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            dev|prod)
                env="$1"
                shift
                ;;
            --rollback)
                action="rollback"
                shift
                ;;
            --status)
                action="status"
                shift
                ;;
            --logs)
                action="logs"
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            v*)
                version="$1"
                shift
                ;;
            *)
                log_error "Option inconnue: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Validation
    if [ "$action" = "deploy" ] && [ -z "$env" ]; then
        log_error "Environnement requis (dev|prod)"
        show_help
        exit 1
    fi
    
    # Exécution
    case $action in
        rollback)
            check_prerequisites
            rollback
            ;;
        status)
            check_prerequisites
            show_status
            ;;
        logs)
            check_prerequisites
            show_logs
            ;;
        deploy)
            check_prerequisites
            build_application
            temp_dir=$(prepare_deployment "$env" "$version")
            deploy_to_server "$env" "$version" "$temp_dir"
            rm -rf "$temp_dir"
            log_success "Déploiement $env terminé avec succès!"
            ;;
    esac
}

# Point d'entrée
main "$@"