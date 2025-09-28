# Script PowerShell pour exporter les diagrammes PlantUML en PNG
# Utilise PlantUML JAR pour générer les images

param(
    [string]$PlantUMLJar = "",
    [string]$InputDir = "docs\views",
    [string]$OutputDir = "docs\arc42"
)

Write-Host "=== Exportation des diagrammes PlantUML vers PNG ===" -ForegroundColor Green

# Vérifier si Java est disponible
try {
    $javaVersion = java -version 2>&1
    Write-Host "Java detecte: $($javaVersion[0])" -ForegroundColor Yellow
} catch {
    Write-Host "ERREUR: Java n'est pas installe ou pas dans le PATH" -ForegroundColor Red
    exit 1
}

# Télécharger PlantUML JAR si nécessaire
$plantUMLJarPath = "plantuml.jar"
if (-not (Test-Path $plantUMLJarPath)) {
    Write-Host "Telechargement de PlantUML..." -ForegroundColor Yellow
    try {
        Invoke-WebRequest -Uri "https://github.com/plantuml/plantuml/releases/download/v1.2024.7/plantuml-1.2024.7.jar" -OutFile $plantUMLJarPath
        Write-Host "PlantUML telecharge avec succes" -ForegroundColor Green
    } catch {
        Write-Host "ERREUR: Impossible de telecharger PlantUML" -ForegroundColor Red
        exit 1
    }
}

# Créer le dossier de sortie s'il n'existe pas
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
    Write-Host "Dossier $OutputDir cree" -ForegroundColor Yellow
}

# Trouver tous les fichiers .puml
$pumlFiles = Get-ChildItem -Path $InputDir -Filter "*.puml" -Recurse

if ($pumlFiles.Count -eq 0) {
    Write-Host "Aucun fichier .puml trouve dans $InputDir" -ForegroundColor Red
    exit 1
}

Write-Host "Fichiers .puml trouves: $($pumlFiles.Count)" -ForegroundColor Yellow

# Traiter chaque fichier
foreach ($file in $pumlFiles) {
    Write-Host "Traitement: $($file.Name)" -ForegroundColor Cyan
    
    try {
        # Générer le PNG avec PlantUML
        $outputPath = Join-Path $OutputDir ($file.BaseName + ".png")
        
        # Commande PlantUML
        java -jar $plantUMLJarPath -tpng -o (Resolve-Path $OutputDir) $file.FullName
        
        if (Test-Path $outputPath) {
            Write-Host "  -> $($file.BaseName).png genere avec succes" -ForegroundColor Green
        } else {
            Write-Host "  -> ERREUR: Echec de generation pour $($file.Name)" -ForegroundColor Red
        }
    } catch {
        Write-Host "  -> ERREUR: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=== Exportation terminee ===" -ForegroundColor Green
Write-Host "Images PNG disponibles dans: $OutputDir" -ForegroundColor Yellow

# Lister les fichiers générés
$pngFiles = Get-ChildItem -Path $OutputDir -Filter "*.png"
if ($pngFiles.Count -gt 0) {
    Write-Host "`nFichiers generes:" -ForegroundColor Yellow
    foreach ($png in $pngFiles) {
        Write-Host "  - $($png.Name)" -ForegroundColor White
    }
}