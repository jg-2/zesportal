@echo off
chcp 65001 >nul
echo ZES Portal - uruchamianie...
echo.
for %%f in (zesportal-*.jar) do (
  rem Uwaga: CharsetProvider (np. Mazovia dla Informix) jest wykrywany przez JVM po system classpath.
  rem Dlatego lib\*.jar dodajemy do -cp, a nie tylko do -Dloader.path.
  java -Dspring.profiles.active=production -Dvaadin.productionMode=true -cp "%%f;lib\*" org.springframework.boot.loader.launch.PropertiesLauncher %*
  goto :done
)
echo Brak pliku JAR. Zbuduj: .\mvnw.cmd clean package "-Pproduction,dist" -DskipTests
pause
:done
