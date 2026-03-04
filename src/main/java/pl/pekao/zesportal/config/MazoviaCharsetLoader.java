package pl.pekao.zesportal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * Przy starcie aplikacji rejestruje charset „Mazovia” (z lib/mazovia.jar), jeśli jest dostępny.
 * Dzięki temu sterownik JDBC Informix widzi ten charset przy połączeniu (UnsupportedCharsetException).
 * Działa tylko gdy mazovia.jar jest na classpath (np. -Dloader.path=lib w run.sh/run.bat).
 */
@Component
@Order(1)
public class MazoviaCharsetLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MazoviaCharsetLoader.class);

    @Override
    public void run(ApplicationArguments args) {
        ClassLoader appLoader = getClass().getClassLoader();
        Thread t = Thread.currentThread();
        ClassLoader previous = t.getContextClassLoader();
        try {
            t.setContextClassLoader(appLoader);
            Charset.forName("Mazovia");
            log.debug("Charset Mazovia zarejestrowany (lib/mazovia.jar)");
        } catch (Exception e) {
            log.trace("Charset Mazovia niedostępny (brak lib/mazovia.jar lub inny provider): {}", e.getMessage());
        } finally {
            t.setContextClassLoader(previous);
        }
    }
}
