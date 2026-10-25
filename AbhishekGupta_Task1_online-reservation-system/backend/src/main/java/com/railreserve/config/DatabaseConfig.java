package com.railreserve.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides JDBC connections to the RailReserve PostgreSQL database.
 *
 * Configuration comes from real environment variables, falling back to a
 * local .env file (gitignored, never committed). Sensible local defaults
 * mean the app can also run against a local PostgreSQL install. No secrets
 * are ever hardcoded or committed.
 *
 * Resolution order for each key (DB_URL, DB_USER, DB_PASSWORD):
 *   1. System environment variable
 *   2. Key in .env file next to the working directory
 *   3. Built-in local default
 */
public final class DatabaseConfig {

    private static volatile Map<String, String> envFileCache;

    private DatabaseConfig() {
    }

    public static String getDbUrl() {
        return resolve("DB_URL", "jdbc:postgresql://localhost:5432/railreserve");
    }

    public static String getDbUser() {
        return resolve("DB_USER", "postgres");
    }

    public static String getDbPassword() {
        return resolve("DB_PASSWORD", "");
    }

    /** Opens a new JDBC connection. Callers are responsible for closing it. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getDbUrl(), getDbUser(), getDbPassword());
    }

    /** Cheap connectivity probe used at startup; true means the DB is reachable. */
    public static boolean isConnectionAvailable() {
        try (Connection ignored = getConnection()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Masked description of the target, safe to show in dialogs/logs. */
    public static String describeTarget() {
        return getDbUrl();
    }

    private static String resolve(String key, String fallback) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = readEnvFile().get(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return fallback;
    }

    /** Minimal .env parser: KEY=VALUE lines, # comments, quoted values tolerated. */
    private static Map<String, String> readEnvFile() {
        Map<String, String> cached = envFileCache;
        if (cached != null) {
            return cached;
        }
        Map<String, String> values = new HashMap<>();
        Path envPath = Path.of(".env");
        if (Files.isRegularFile(envPath)) {
            try (InputStream in = Files.newInputStream(envPath)) {
                for (String line : new String(in.readAllBytes()).split("\\R")) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    int eq = trimmed.indexOf('=');
                    if (eq > 0) {
                        String key = trimmed.substring(0, eq).trim();
                        String value = trimmed.substring(eq + 1).trim();
                        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                            value = value.substring(1, value.length() - 1);
                        }
                        values.put(key, value);
                    }
                }
            } catch (IOException ignored) {
                // Fall back to defaults when .env is unreadable
            }
        }
        envFileCache = Map.copyOf(values);
        return envFileCache;
    }
}
