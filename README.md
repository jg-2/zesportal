# ZES Portal

Aplikacja webowa (Vaadin, Spring Boot) – wywołania usług Tuxedo (JOLT), konfiguracja usług, definicje importów, zadania.

## Wymagania

- **Java 17+**
- **Maven 3.6+**

## Uruchomienie na nowym komputerze (czysty build, pusta baza)

Po sklonowaniu repozytorium baza danych **nie jest** w repo – przy pierwszym uruchomieniu H2 utworzy pustą bazę w katalogu `./data/`.

```bash
# Klonowanie
git clone https://github.com/TWOJ_USER/zesportal.git
cd zesportal

# Czysty build (bez starych artefaktów)
mvn clean install

# Uruchomienie aplikacji
mvn spring-boot:run
```

Albo z gotowego JAR (po `mvn clean package`):

```bash
java -jar target/zesportal-*.jar
```

Aplikacja będzie dostępna pod adresem: **http://localhost:8080**

- Baza H2: `./data/zesportal` (tworzona przy pierwszym starcie, pusta)
- Konsola H2 (podgląd bazy): http://localhost:8080/h2-console  
  JDBC URL: `jdbc:h2:file:./data/zesportal`, user: `sa`, hasło: puste

## Konfiguracja

- `src/main/resources/application.properties` – ustawienia główne
- `test.jolt=true` – tryb testowy Tuxedo (symulacja bez rzeczywistego połączenia JOLT)

## Co nie trafia do Gita (czysty build u Ciebie i u innych)

- `target/` – build Maven
- `data/` – pliki bazy H2 (każde uruchomienie może mieć swoją lokalną bazę)
- `.idea/`, `*.iml` – ustawienia IDE

Dzięki temu po sklonowaniu na innym komputerze i uruchomieniu `mvn clean install && mvn spring-boot:run` dostaniesz **czystą aplikację z pustą bazą**.
