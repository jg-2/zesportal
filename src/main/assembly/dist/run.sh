#!/bin/sh
echo "ZES Portal - uruchamianie..."
JAR=$(ls zesportal-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
  echo "Brak pliku JAR. Zbuduj: ./mvnw clean package \"-Pproduction,dist\" -DskipTests"
  exit 1
fi
# Uwaga: CharsetProvider (np. Mazovia dla Informix) jest wykrywany przez JVM po system classpath.
# Dlatego lib/*.jar dodajemy do -cp, a nie tylko do -Dloader.path.
exec java -Dspring.profiles.active=production -Dvaadin.productionMode=true -cp "$JAR:lib/*" org.springframework.boot.loader.launch.PropertiesLauncher "$@"
