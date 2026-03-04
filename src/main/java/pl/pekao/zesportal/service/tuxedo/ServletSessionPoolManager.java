package pl.pekao.zesportal.service.tuxedo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.pekao.zesportal.config.JoltPoolProperties;
import pl.pekao.zesportal.entity.ServerService;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jak w działającym przykładzie JOLT: {@code bea.jolt.pool.servlet.ServletSessionPoolManager},
 * {@code createSessionPool(addr, null, poolMin, poolMax, ui, spName)}, {@code getSessionPool(spName)},
 * wywołania przez {@code sp.call(serviceName, ds, null)}. Bez własnej sesji – wyłącznie API bea.jolt.
 */
@Component
public class ServletSessionPoolManager {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ServletSessionPoolManager.class);

    /** bea.jolt.pool.servlet.ServletSessionPoolManager – z joltse.jar (jak w przykładzie) */
    private static final String[] POOL_MGR_CLASS_NAMES = {
            "bea.jolt.pool.servlet.ServletSessionPoolManager"
    };
    /** bea.jolt.pool.servlet.ServletSessionPool */
    private static final String[] SESSION_POOL_CLASS_NAMES = {
            "bea.jolt.pool.servlet.ServletSessionPool"
    };
    /** Możliwe klasy UserInfo */
    private static final String[] USER_INFO_CLASS_NAMES = { "bea.jolt.pool.UserInfo", "bea.jolt.UserInfo" };
    /** Możliwe klasy DataSet */
    private static final String[] DATA_SET_CLASS_NAMES = { "bea.jolt.pool.DataSet", "bea.jolt.DataSet" };

    private final JoltPoolProperties poolProperties;
    private final Map<String, PoolEntry> pools = new ConcurrentHashMap<>();

    private Class<?> poolManagerClass;
    private Object poolManagerInstance;
    private Method createSessionPoolMethod;
    private Method getSessionPoolMethod;
    private Method poolCallMethod;
    private String userInfoClassName;
    private String dataSetClassName;
    /** Trzeci parametr pool.call(serviceName, dataSet, third) – jeśli boolean, przekazujemy false zamiast null */
    private Class<?> poolCallThirdParamType;
    /** ClassLoader JOLT (pula) – DataSet musi być z tego samego loadera, inaczej argument type mismatch */
    private ClassLoader joltClassLoader;

    public ServletSessionPoolManager(JoltPoolProperties poolProperties) {
        this.poolProperties = poolProperties;
        initJoltPoolReflection();
    }

    private void initJoltPoolReflection() {
        Exception lastException = null;
        for (String poolMgrName : POOL_MGR_CLASS_NAMES) {
            try {
                poolManagerClass = Class.forName(poolMgrName);
                poolManagerInstance = poolManagerClass.getDeclaredConstructor().newInstance();
                getSessionPoolMethod = poolManagerClass.getMethod("getSessionPool", String.class);

                // createSessionPool(addr, ..., poolMin, poolMax, ui, spName) – 6 parametrów; 2. parametr to String[] lub Object (w przykładzie: null)
                createSessionPoolMethod = null;
                for (Method m : poolManagerClass.getMethods()) {
                    if (!"createSessionPool".equals(m.getName()) || m.getParameterCount() != 6) continue;
                    Class<?>[] pt = m.getParameterTypes();
                    if (pt[0] == String[].class && (pt[1] == Object.class || pt[1] == String[].class)
                            && pt[2] == int.class && pt[3] == int.class && pt[5] == String.class) {
                        createSessionPoolMethod = m;
                        break;
                    }
                }
                if (createSessionPoolMethod == null) {
                    log.warn("JOLT: w {} nie znaleziono metody createSessionPool(String[], Object, int, int, UserInfo, String). Dostępne metody createSessionPool: {}",
                            poolMgrName, listCreateSessionPoolSignatures(poolManagerClass));
                    lastException = new IllegalStateException("createSessionPool(String[], Object, int, int, *, String) nie znaleziona");
                    continue;
                }

                Class<?> userInfoParam = createSessionPoolMethod.getParameterTypes()[4];
                userInfoClassName = userInfoParam.getName();

                // DataSet – potrzebne do wyboru właściwej metody call(serviceName, dataSet, transaction)
                dataSetClassName = null;
                Class<?> dataSetClass = null;
                for (String dsName : DATA_SET_CLASS_NAMES) {
                    try {
                        Class<?> c = Class.forName(dsName);
                        c.getDeclaredConstructor().newInstance();
                        dataSetClassName = dsName;
                        dataSetClass = c;
                        break;
                    } catch (Exception e) {
                        lastException = e;
                    }
                }
                if (dataSetClassName == null || dataSetClass == null) {
                    log.warn("JOLT: nie znaleziono klasy DataSet (no-arg constructor) w: {}. Ostatni błąd: {}", (Object) DATA_SET_CLASS_NAMES, lastException != null ? lastException.getMessage() : "brak");
                    continue;
                }

                poolCallMethod = null;
                poolCallThirdParamType = null;
                for (String poolClassName : SESSION_POOL_CLASS_NAMES) {
                    try {
                        Class<?> sessionPoolClass = Class.forName(poolClassName);
                        for (Method m : sessionPoolClass.getMethods()) {
                            if (!"call".equals(m.getName()) || m.getParameterCount() != 3) continue;
                            Class<?>[] pt = m.getParameterTypes();
                            // 2. parametr musi przyjmować nasz DataSet; 3. – zapisujemy (null vs boolean)
                            if (pt[0] == String.class && pt[1].isAssignableFrom(dataSetClass)) {
                                poolCallMethod = m;
                                poolCallThirdParamType = pt[2];
                                break;
                            }
                        }
                        if (poolCallMethod != null) break;
                    } catch (ClassNotFoundException e) {
                        lastException = e;
                    }
                }
                if (poolCallMethod == null) {
                    log.warn("JOLT: nie znaleziono metody call(String, DataSet, *) w SessionPool (klasy: {})", (Object) SESSION_POOL_CLASS_NAMES);
                    if (lastException == null) lastException = new NoSuchMethodException("call(String, DataSet, *)");
                    continue;
                }
                joltClassLoader = poolCallMethod.getDeclaringClass().getClassLoader();

                log.info("JOLT: użyto {} (pula: {}), UserInfo={}, DataSet={}",
                        poolMgrName, poolCallMethod.getDeclaringClass().getName(), userInfoClassName, dataSetClassName);
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("JOLT: błąd inicjalizacji {}: {}", poolMgrName, e.getMessage());
            }
        }
        log.warn("JOLT ServletSessionPoolManager nie został znaleziony. Sprawdź lib/ (jolt.jar, joltse.jar). Ostatni błąd: {}",
                lastException != null ? lastException.toString() : "brak");
        if (lastException != null && log.isDebugEnabled()) {
            log.debug("Szczegóły:", lastException);
        }
    }

    private static String listCreateSessionPoolSignatures(Class<?> poolManagerClass) {
        StringBuilder sb = new StringBuilder();
        for (Method m : poolManagerClass.getMethods()) {
            if (!"createSessionPool".equals(m.getName())) continue;
            sb.append("(");
            Class<?>[] pt = m.getParameterTypes();
            for (int i = 0; i < pt.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(pt[i].getSimpleName());
            }
            sb.append("); ");
        }
        return sb.length() > 0 ? sb.toString() : "brak";
    }

    private boolean isPoolApiAvailable() {
        return poolManagerInstance != null && createSessionPoolMethod != null && getSessionPoolMethod != null && poolCallMethod != null;
    }

    /**
     * Zwraca pulę dla komponentu (jak w przykładzie: ServletSessionPool sp = (ServletSessionPool) spm.getSessionPool(spName)).
     * Przy pierwszym wywołaniu dla danego komponentu wywoływane jest createSessionPool(addr, null, poolMin, poolMax, ui, spName),
     * więc połączenie z JSL jest nawiązywane w tym momencie (albo przy pierwszym sp.call() – zależnie od implementacji JOLT).
     */
    public Object getSessionPool(ServerService serverService) throws Exception {
        if (serverService == null || serverService.getId() == null) {
            throw new IllegalArgumentException("ServerService i id muszą być ustawione");
        }
        if (!isPoolApiAvailable()) {
            throw new IllegalStateException(
                    "JOLT ServletSessionPoolManager nie jest dostępny. Upewnij się, że uruchamiasz z run.bat/run.sh (ładują lib/ przez -Dloader.path=lib) " +
                            "i że w lib/ są jolt.jar oraz joltse.jar. Sprawdź logi przy starcie (WARN) – tam jest dokładny powód.");
        }
        String key = poolKey(serverService);
        // Pierwsze wywołanie dla tego komponentu: createPool() -> createSessionPool() -> połączenie z JSL
        PoolEntry entry = pools.computeIfAbsent(key, k -> {
            try {
                return createPool(serverService, k);
            } catch (Exception ex) {
                throw new RuntimeException("createSessionPool failed: " + ex.getMessage(), ex);
            }
        });
        entry.lastAccessMs = System.currentTimeMillis();
        return entry.pool;
    }

    /**
     * Wywołanie serwisu jak w przykładzie: callRes = sp.call(serviceName, ds, null).
     *
     * @param serverService komponent (pula)
     * @param serviceName   nazwa usługi Tuxedo (np. IMRachFinCzytaj)
     * @param dataSet      bea.jolt.pool.DataSet z parametrami (np. ds.setValue("NR_ODDZ", "901"))
     * @return bea.jolt.pool.Result (callRes.getApplicationCode(), callRes.getValue(...))
     */
    public Object call(ServerService serverService, String serviceName, Object dataSet) throws Exception {
        Object pool = getSessionPool(serverService);
        Object thirdArg = (poolCallThirdParamType != null
                && (poolCallThirdParamType == boolean.class || poolCallThirdParamType == Boolean.class))
                ? Boolean.FALSE
                : null;
        try {
            logRecordset(serviceName, "WEJŚCIE", dataSet);
            Object result = poolCallMethod.invoke(pool, serviceName, dataSet, thirdArg);
            logRecordset(serviceName, "WYJŚCIE", result);
            return result;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                String msg = formatJoltException(serviceName, cause);
                if (cause instanceof RuntimeException re) throw new IllegalStateException(msg, re);
                if (cause instanceof Error err) throw new IllegalStateException(msg, err);
                throw new IllegalStateException(msg, cause);
            }
            throw e;
        } catch (IllegalArgumentException e) {
            Class<?>[] expected = poolCallMethod.getParameterTypes();
            String msg = String.format("JOLT call argument type mismatch. Oczekiwane typy: (String, %s, %s). " +
                            "Przekazano: serviceName=%s, dataSet=%s (classLoader=%s), third=%s.",
                    expected.length > 1 ? expected[1].getName() : "?", expected.length > 2 ? expected[2].getName() : "?",
                    serviceName != null ? serviceName.getClass().getName() : "null",
                    dataSet != null ? dataSet.getClass().getName() : "null",
                    dataSet != null ? dataSet.getClass().getClassLoader() : "null",
                    thirdArg);
            throw new IllegalArgumentException(msg, e);
        }
    }

    /**
     * Buduje czytelny komunikat z wyjątku JOLT (ApplicationException ma getApplicationCode/getResult, ServiceException getErrno).
     */
    private String formatJoltException(String serviceName, Throwable cause) {
        String prefix = "JOLT" + (serviceName != null && !serviceName.isBlank() ? " [" + serviceName + "]" : "") + ": ";
        String base = cause.getMessage() != null && !cause.getMessage().isBlank() ? cause.getMessage() : cause.getClass().getSimpleName();
        try {
            Class<?> c = cause.getClass();
            if (c.getName().contains("ApplicationException")) {
                StringBuilder sb = new StringBuilder(prefix).append("Błąd aplikacji Tuxedo (np. błędne dane wejściowe). ");
                try {
                    Method m = c.getMethod("getApplicationCode");
                    Object code = m.invoke(cause);
                    if (code != null) sb.append("Kod błędu: ").append(code).append(". ");
                } catch (Exception ignored) { }
                try {
                    Method m = c.getMethod("getResult");
                    Object result = m.invoke(cause);
                    if (result != null) sb.append("Wynik/opis: ").append(result);
                } catch (Exception ignored) { }
                if (sb.length() > prefix.length() + 50) return sb.toString();
            }
            if (c.getName().contains("ServiceException")) {
                try {
                    Method m = c.getMethod("getErrno");
                    Object errno = m.invoke(cause);
                    if (errno != null) return prefix + base + " (errno=" + errno + ")";
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        return prefix + base;
    }

    /**
     * Konwertuje JOLT DataSet/Result na mapę klucz–wartość (do zapisu w rezultacie zadania).
     * Wartości nieserializowalne do JSON są zamieniane na string (toString).
     */
    public Map<String, Object> recordsetToMap(Object dataSetOrResult) {
        Map<String, Object> sorted = new TreeMap<>();
        if (dataSetOrResult == null) return sorted;
        try {
            Class<?> cl = dataSetOrResult.getClass();
            Method keySetMethod = cl.getMethod("keySet");
            @SuppressWarnings("unchecked")
            Set<Object> keys = (Set<Object>) keySetMethod.invoke(dataSetOrResult);
            if (keys == null || keys.isEmpty()) return sorted;
            Method getValueMethod;
            try {
                getValueMethod = cl.getMethod("getValue", String.class, Object.class);
            } catch (NoSuchMethodException e) {
                getValueMethod = cl.getMethod("get", Object.class);
            }
            for (Object keyObj : keys) {
                if (keyObj == null) continue;
                String key = keyObj.toString();
                Object val;
                if (getValueMethod.getParameterCount() == 2) {
                    val = getValueMethod.invoke(dataSetOrResult, key, null);
                } else {
                    val = getValueMethod.invoke(dataSetOrResult, keyObj);
                }
                sorted.put(key, toJsonSafeValue(val));
            }
        } catch (Exception e) {
            log.debug("recordsetToMap: {}", e.getMessage());
        }
        return sorted;
    }

    private static Object toJsonSafeValue(Object val) {
        if (val == null) return null;
        if (val instanceof String || val instanceof Number || val instanceof Boolean) return val;
        if (val instanceof Map || val instanceof Collection) return val;
        return val.toString();
    }

    /**
     * Konwertuje JOLT DataSet/Result na JSON (pełny recordset do zapisu w rezultacie zadania).
     */
    public String recordsetToJson(Object dataSetOrResult) {
        if (dataSetOrResult == null) return "{}";
        try {
            Map<String, Object> map = recordsetToMap(dataSetOrResult);
            return JSON.writeValueAsString(map);
        } catch (Exception e) {
            log.debug("recordsetToJson: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Zrzut recordsetu (wejście lub wyjście) na konsolę serwera.
     */
    private void logRecordset(String serviceName, String direction, Object dataSetOrResult) {
        if (dataSetOrResult == null) {
            log.info("JOLT [{}] {}: (null)", serviceName, direction);
            return;
        }
        Map<String, Object> map = recordsetToMap(dataSetOrResult);
        if (map.isEmpty()) {
            log.info("JOLT [{}] {}: (pusty recordset)", serviceName, direction);
            return;
        }
        try {
            log.info("JOLT [{}] {}:\n{}", serviceName, direction, JSON.writeValueAsString(map));
        } catch (Exception e) {
            log.info("JOLT [{}] {}: (błąd serializacji: {})", serviceName, direction, e.getMessage());
        }
    }

    /**
     * Jak w przykładzie: DataSet ds = new DataSet(); ds.setValue(...); callRes = sp.call(serviceName, ds, null).
     */
    public Object call(ServerService serverService, String serviceName, Map<String, Object> params) throws Exception {
        Object dataSet = createDataSet(params);
        return call(serverService, serviceName, dataSet);
    }

    /** Tworzy DataSet w ClassLoaderze JOLT (ta sama instancja co pula), żeby uniknąć argument type mismatch. */
    public Object createDataSet(Map<String, Object> params) throws Exception {
        String dsClass = dataSetClassName != null ? dataSetClassName : DATA_SET_CLASS_NAMES[0];
        Class<?> dsCl = joltClassLoader != null
                ? Class.forName(dsClass, true, joltClassLoader)
                : Class.forName(dsClass);
        Object ds = dsCl.getDeclaredConstructor().newInstance();
        Method setValue = dsCl.getMethod("setValue", String.class, Object.class);
        if (params != null) {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                if (e.getKey() != null) setValue.invoke(ds, e.getKey(), e.getValue());
            }
        }
        return ds;
    }

    private PoolEntry createPool(ServerService serverService, String poolName) throws Exception {
        String[] addr = resolveAddresses(serverService);
        String uiClass = userInfoClassName != null ? userInfoClassName : USER_INFO_CLASS_NAMES[0];
        Object userInfo = Class.forName(uiClass).getDeclaredConstructor().newInstance();
        int poolMin = 1;
        int poolMax = Math.max(1, poolProperties.getMaxSize());

        log.info("JOLT createSessionPool (jak w przykładzie), komponent [{}]: addr={}, poolMin={}, poolMax={}, poolName={}",
                serverService.getName(), String.join(",", addr), poolMin, poolMax, poolName);

        createSessionPoolMethod.invoke(poolManagerInstance, addr, null, poolMin, poolMax, userInfo, poolName);
        Object pool = getSessionPoolMethod.invoke(poolManagerInstance, poolName);
        if (pool == null) {
            throw new IllegalStateException("getSessionPool zwróciło null dla " + poolName);
        }
        return new PoolEntry(pool);
    }

    /** Tablica adresów: [primary] lub [primary, backup]. */
    private String[] resolveAddresses(ServerService component) {
        String primary = resolveAppAddress(component);
        String backup = resolveBackupAddress(component);
        return backup != null ? new String[]{primary, backup} : new String[]{primary};
    }

    private String resolveAppAddress(ServerService component) {
        String host = component.getServer() != null ? component.getServer().getAddress() : null;
        if (host == null || host.isBlank()) throw new IllegalArgumentException("Brak adresu serwera w komponencie.");
        int port = component.getPort() != null ? component.getPort() : 0;
        if (port <= 0 && component.getConfig() != null && !component.getConfig().isBlank()) {
            try {
                JsonNode n = JSON.readTree(component.getConfig());
                if (n.has("port")) port = n.get("port").asInt();
            } catch (Exception ignored) { }
        }
        if (port <= 0) throw new IllegalArgumentException("Brak portu w komponencie.");
        return "//" + host.trim() + ":" + port;
    }

    private String resolveBackupAddress(ServerService component) {
        if (component.getConfig() == null || component.getConfig().isBlank()) return null;
        try {
            JsonNode n = JSON.readTree(component.getConfig());
            if (n.has("backupAddress") && n.get("backupAddress").isTextual()) {
                return n.get("backupAddress").asText().trim();
            }
        } catch (Exception ignored) { }
        return null;
    }

    @Scheduled(fixedDelayString = "${jolt.pool.idle-check-interval-ms:60000}")
    public void closeIdlePools() {
        if (poolProperties.getIdleTimeoutSeconds() <= 0) return;
        try {
            Method stopPool = poolManagerClass != null ? poolManagerClass.getMethod("stopSessionPool", String.class) : null;
            if (stopPool == null) return;
            long idleMs = poolProperties.getIdleTimeoutSeconds() * 1000L;
            Iterator<Map.Entry<String, PoolEntry>> it = pools.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, PoolEntry> e = it.next();
                if ((System.currentTimeMillis() - e.getValue().lastAccessMs) > idleMs) {
                    try {
                        stopPool.invoke(poolManagerInstance, e.getKey());
                    } catch (Exception ex) {
                        log.debug("stopSessionPool({}): {}", e.getKey(), ex.getMessage());
                    }
                    it.remove();
                    log.debug("Usunięto pulę JOLT [{}]", e.getKey());
                }
            }
        } catch (Exception e) {
            log.trace("closeIdlePools: {}", e.getMessage());
        }
    }

    private static String poolKey(ServerService ss) {
        return "ss-" + ss.getId();
    }

    private static final class PoolEntry {
        final Object pool;
        volatile long lastAccessMs = System.currentTimeMillis();

        PoolEntry(Object pool) {
            this.pool = pool;
        }
    }
}
