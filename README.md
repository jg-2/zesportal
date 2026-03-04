# ZES Portal

Aplikacja webowa (Vaadin, Spring Boot) – wywołania usług Tuxedo (JOLT), konfiguracja usług, definicje importów, zadania.

## Wymagania

- **Java 17+** (Maven jest w projekcie jako wrapper – `mvnw.cmd` / `mvnw`, nie musisz instalować Mavena)

---

## Pobranie gotowego builda (ZIP)

Jeśli w repozytorium są **Releases** (zakładka *Releases* na GitHubie):

1. Wejdź w **Releases** i pobierz najnowszy plik **`zesportal-*-dist.zip`**.
2. Rozpakuj archiwum.
3. **Windows:** uruchom `run.bat`  
   **Linux/Mac:** `chmod +x run.sh && ./run.sh`
4. Otwórz przeglądarkę: **http://localhost:8080**

W archiwum jest kompletny build (JAR + skrypty + instrukcja). Baza H2 tworzy się przy pierwszym starcie (pusta).

---

## Budowanie ZIP do wrzucenia na Git / Releases

W katalogu projektu (wystarczy Java 17, build przez Maven Wrapper):

**Windows (CMD lub PowerShell):**
```cmd
.\mvnw.cmd clean package "-Pproduction,dist" -DskipTests
```

**Linux / Mac:**
```bash
./mvnw clean package "-Pproduction,dist" -DskipTests
```

W katalogu **`target/`** pojawi się plik:

**`zesportal-0.0.1-SNAPSHOT-dist.zip`**

**Zawartość ZIP:**
- `zesportal/zesportal-0.0.1-SNAPSHOT.jar` – aplikacja (fat JAR)
- `zesportal/application.properties` – konfiguracja **na zewnątrz** JAR-a (można edytować bez ponownego builda)
- `zesportal/lib/` – katalog na dodatkowe JAR-y; **dołóż tutaj jolt.jar** (Oracle Tuxedo JOLT), jeśli chcesz używać prawdziwego połączenia z Tuxedo (opcjonalnie, później)
- `zesportal/run.bat`, `zesportal/run.sh` – uruchomienie (ładują JAR-y z `lib/`)
- `zesportal/URUCHOMIENIE.txt` – instrukcja

### Wrzucenie na GitHub jako Release (do ściągnięcia w formacie ZIP)

1. Zbuduj ZIP: `.\mvnw.cmd clean package "-Pproduction,dist" -DskipTests` (Windows) lub `./mvnw clean package "-Pproduction,dist" -DskipTests` (Linux/Mac)
2. Na GitHubie: repozytorium → **Releases** → **Create a new release**.
3. **Tag:** np. `v1.0` (albo np. `v0.0.1`).
4. **Release title:** np. `ZES Portal 1.0`.
5. W sekcji **Attach binaries** przeciągnij plik **`target/zesportal-0.0.1-SNAPSHOT-dist.zip`** (albo zmień wersję w `pom.xml` na `1.0` i zbuduj ponownie, wtedy plik będzie `zesportal-1.0-dist.zip`).
6. Kliknij **Publish release**.

Od teraz użytkownicy mogą wejść w **Releases** i pobrać **zesportal-*-dist.zip** – kompletny build do uruchomienia bez Mavena.

---

## Uruchomienie ze źródeł (czysty build, pusta baza)

Po sklonowaniu repozytorium baza danych **nie jest** w repo – przy pierwszym uruchomieniu H2 utworzy pustą bazę w katalogu `./data/`.

```bash
git clone https://github.com/TWOJ_USER/zesportal.git
cd zesportal
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```
(Linux/Mac: zamiast `.\mvnw.cmd` użyj `./mvnw`.)

**Uruchomienie JAR (java -jar):** JAR **musi** być zbudowany z profilem **production**, inaczej przy starcie wystąpi błąd „Failed to find ... jar-resources”. Zbuduj i uruchom:

```bash
.\mvnw.cmd clean package "-Pproduction" -DskipTests
java -jar target/zesportal-*.jar
```
(Linux/Mac: `./mvnw clean package "-Pproduction" -DskipTests`.)

(Lub z parametrem: `java -Dvaadin.productionMode=true -jar target/zesportal-*.jar` – i tak wymagany jest build z `-Pproduction`, żeby frontend był w JARze.)

Aplikacja: **http://localhost:8080**

- Baza H2: `./data/zesportal` (tworzona przy pierwszym starcie)
- Konsola H2: http://localhost:8080/h2-console (user: `sa`, hasło: puste)

## Konfiguracja

- **W źródłach:** `src/main/resources/application.properties`
- **W buildzie (ZIP):** `application.properties` leży **obok JAR-a** – możesz go edytować po rozpakowaniu (port, baza, `test.jolt` itd.) bez ponownego builda.
- `test.jolt=true` – tryb testowy Tuxedo (symulacja bez JOLT). Ustaw `false` i dołóż `jolt.jar` do `lib/`, aby łączyć się z Tuxedo.

### Biblioteka JOLT (jolt.jar) – dołożenie później

Domyślnie aplikacja działa **bez** `jolt.jar` (tryb testowy). Aby używać prawdziwego połączenia JOLT:

1. W **rozpakowanym ZIP** (albo obok JAR-a): skopiuj `jolt.jar` z instalacji Oracle Tuxedo do katalogu **`lib/`** (obok JAR-a). W ZIP jest już katalog `lib/` z krótką instrukcją.
2. W **application.properties** (tym na zewnątrz) ustaw: `test.jolt=false`.
3. Uruchom przez `run.bat` / `run.sh` – skrypty ustawiają `-Dloader.path=lib`, więc JAR-y z `lib/` (w tym `jolt.jar`) są ładowane przy starcie.

Nie musisz nic przebudowywać – bibliotekę dołączasz po stronie wdrożenia.

---

## Uruchomienie z IDE (profil dev – brak błędu „jar-resources”)

Jeśli przy starcie pojawia się błąd **Failed to find the following files: .../generated/jar-resources/...**, Vaadin oczekuje wygenerowanego frontendu (production). Uruchom aplikację **z profilem dev**:

- **Cursor / VS Code:** uruchom konfigurację **„ZES Portal (profil dev)”** z panelu Run and Debug (F5). W pliku `.vscode/launch.json` jest ustawione `-Dspring.profiles.active=dev`.
- **Inne IDE:** w konfiguracji uruchomienia (Run Configuration) dodaj w VM options: `-Dspring.profiles.active=dev`
- **Z wiersza poleceń (bez IDE):** w katalogu głównym projektu (tam gdzie jest `pom.xml`) uruchom `.\run-dev.bat` (Windows) albo `.\mvnw.cmd spring-boot:run` (Maven domyślnie używa profilu dev). Skrypt `run-dev.bat` leży w **korzeniu projektu** (obok `pom.xml`, `mvnw.cmd`).

W trybie dev Vaadin nie wymaga wcześniej zbudowanego frontendu. JAR z dystrybucji (ZIP) uruchamiaj przez `run.bat` / `run.sh` – tam ustawiony jest profil `production` i frontend jest wewnątrz JAR-a.

---

## Błąd „Column TUXEDO_NO_AUTH not found”

Jeśli po wejściu w **Komponenty systemowe** pojawia się błąd **Column "TUXEDO_NO_AUTH" not found**, w bazie H2 brakuje kolumn dodanych dla autentykacji Tuxedo. Zrób jedno z poniższych:

1. **Restart aplikacji** – przy `ddl-auto=update` Hibernate może dodać kolumny przy starcie. Zatrzymaj aplikację i uruchom ją ponownie.
2. **Ręczne dodanie kolumn** – wejdź na **http://localhost:8080/h2-console**, połącz się (JDBC URL: `jdbc:h2:file:./data/zesportal`, user: `sa`, hasło: puste) i wykonaj:
   ```sql
   ALTER TABLE server_service ADD COLUMN tuxedo_no_auth BOOLEAN DEFAULT TRUE;
   ALTER TABLE server_service ADD COLUMN tuxedo_password VARCHAR(500);
   ```
   Pełny skrypt: `src/main/resources/db/migration/V1__add_tuxedo_auth_columns.sql`. Po wykonaniu odśwież stronę Komponentów systemowych.

---

## Biblioteki i katalog lib/

Dystrybucja ZIP zawiera fat JAR oraz katalog **lib/** obok JAR-a. Aplikacja uruchamiana przez `run.bat`/`run.sh` ładuje dodatkowe JAR-y z `lib/` (PropertiesLauncher, `-Dloader.path=lib`). Do **lib/** dokładaj według potrzeb: **jolt.jar** (Tuxedo), **sterowniki JDBC** (np. `postgresql-*.jar`, sterownik Informix) – bez wpisywania ich w pom.xml; **application.properties** jest na zewnątrz JAR-a i także można go edytować po wdrożeniu.

### Informix i charset Mazovia (UnsupportedCharsetException: Mazovia)

Jeśli przy **teście połączenia** do bazy Informix w charsetcie Mazovia pojawia się **UnsupportedCharsetException: Mazovia**, mogą być dwie przyczyny:

1. **Katalog lib/ nie jest na classpath**  
   - **Uruchomienie z ZIP (run.bat / run.sh):** `-Dloader.path=lib` ładuje JAR-y z `lib/`, więc `mazovia.jar` w `lib/` jest widoczny.  
   - **Uruchomienie ze źródeł (run-dev.bat / `mvn spring-boot:run`):** Maven **nie** dodaje `lib/` do classpath. Masz dwie opcje:
     - **Opcja A:** Uruchamiaj zbudowany JAR z loaderem (tak jak w produkcji):  
       `mvnw package -Pproduction -DskipTests`  
       potem:  
       `java -Dloader.path=lib -Dspring.profiles.active=dev -jar target/zesportal-*.jar`  
       Wtedy JAR-y z `lib/` (w tym `mazovia.jar`) są ładowane.
     - **Opcja B:** Użyj profilu Maven **mazovia** (wymaga pliku `lib/mazovia.jar`):  
       `mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev" -Pmazovia`  
       albo w `run-dev.bat` dopisz `-Pmazovia` do wywołania Mavena.

2. **JAR mazovia.jar nie rejestruje charsetu**  
   Java ładuje niestandardowe zestawy znaków przez **CharsetProvider** (SPI). W `mazovia.jar` musi być plik:
   - **Ścieżka w JAR:** `META-INF/services/java.nio.charset.spi.CharsetProvider`
   - **Zawartość:** jedna linia z pełną nazwą klasy implementującej `CharsetProvider` (np. `pl.example.MazoviaCharsetProvider`).

   Sprawdzenie: `jar tf lib/mazovia.jar | findstr CharsetProvider` (Windows) lub `jar tf lib/mazovia.jar | grep CharsetProvider` (Linux/Mac). Powinna pojawić się ścieżka `META-INF/services/java.nio.charset.spi.CharsetProvider`.  
   Jeśli jej nie ma, ten JAR nie zarejestruje charsetu „Mazovia” – trzeba użyć innego JAR-a (np. z biblioteki ICU4J lub dedykowanego providera Mazovia) albo dodać ten plik i odpowiednią klasę do JAR-a.
