# Uruchomienie Zesportal w trybie deweloperskim (do pokazow)
# Wymaga: wczesniej wykonane mvn clean package (profil dev jest domyslny)

$jar = "target\zesportal-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
    Write-Host "Budowanie projektu (profil dev)..."
    mvn clean package -q
}
Write-Host "Uruchamianie z profilem dev..."
java -Dspring.profiles.active=dev -jar $jar
