-- Ręczna migracja: kolumny autentykacji Tuxedo (tuxedo_no_auth, tuxedo_password).
-- Uruchom w H2 Console (http://localhost:8080/h2-console), JDBC URL: jdbc:h2:file:./data/zesportal, user: sa, hasło: puste.
-- Gdy pojawi się błąd "Column TUXEDO_NO_AUTH not found": zatrzymaj aplikację, uruchom poniższe SQL w H2 Console (połącz się do pliku ./data/zesportal), potem uruchom aplikację ponownie.
-- (Jeśli kolumna już istnieje, zobaczysz błąd "already exists" – wtedy pomiń tę linię.)

ALTER TABLE server_service ADD COLUMN tuxedo_no_auth BOOLEAN DEFAULT TRUE;
ALTER TABLE server_service ADD COLUMN tuxedo_password VARCHAR(500);
