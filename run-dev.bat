@echo off
chcp 65001 >nul
echo ZES Portal - tryb deweloperski (profil dev, Vaadin bez production build)...
echo Przy Informix + charset Mazovia dodaj -Pmazovia (wymaga lib\mazovia.jar)
echo.
call mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"
pause
