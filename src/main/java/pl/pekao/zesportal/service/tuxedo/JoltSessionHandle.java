package pl.pekao.zesportal.service.tuxedo;

import java.lang.reflect.Method;

/**
 * Lease sesji z puli JOLT – wyłącznie zwalnia sesję z powrotem do puli (releaseSession).
 * <strong>Nie definiujemy własnej sesji.</strong> Obiekt sesji to zawsze instancja z bea.jolt
 * (zwrócona przez {@code bea.jolt.pool.SessionPool.getSession()}).
 * <p>
 * Pełna nazwa klasy sesji: {@value #JOLT_SESSION_CLASS}. Po zakończeniu użycia wywołaj {@link #close()}.
 */
public class JoltSessionHandle implements AutoCloseable {

    /** Klasa sesji z bea.jolt – do rzutowania gdy jolt.jar jest na classpath. */
    public static final String JOLT_SESSION_CLASS = "bea.jolt.JoltSession";

    /** Sesja z bea.jolt (SessionPool.getSession()) – nie nasz typ, wyłącznie bea.jolt. */
    private final Object session;
    private final Object sessionPool;
    private final Method releaseSessionMethod;
    private boolean released;

    public JoltSessionHandle(Object session, Object sessionPool, Method releaseSessionMethod) {
        this.session = session;
        this.sessionPool = sessionPool;
        this.releaseSessionMethod = releaseSessionMethod;
        this.released = false;
    }

    /**
     * Zwraca sesję z bea.jolt (ta sama referencja co z SessionPool.getSession()).
     * Typ w runtime to {@value #JOLT_SESSION_CLASS}. Rzutowanie: (JoltSession) handle.getSession() przy jolt.jar.
     */
    public Object getSession() {
        return session;
    }

    /** Zwraca sesję do puli przez releaseSession (bea.jolt.pool.SessionPool). */
    @Override
    public void close() {
        if (released) return;
        released = true;
        try {
            if (releaseSessionMethod != null && sessionPool != null) {
                releaseSessionMethod.invoke(sessionPool, session);
            }
        } catch (Exception e) {
            // releaseSession w bea.jolt
        }
    }
}
