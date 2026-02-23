@echo off
REM Uruchomienie Zesportal w trybie deweloperskim (do pokazow)
REM Wymaga: wczesniej wykonane mvn clean package (profil dev jest domyslny)

set JAR=target\zesportal-0.0.1-SNAPSHOT.jar
if not exist "%JAR%" (
  echo Budowanie projektu (profil dev)...
  call mvn clean package -q
)
echo Uruchamianie z profilem dev...
java -Dspring.profiles.active=dev -jar "%JAR%"
