package com.railreserve.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Local UI preferences stored in a small properties file
 * (~/.railreserve/preferences.properties by default): light/dark theme,
 * default travel class and default source station. Load is deliberately
 * tolerant — a missing or corrupt file falls back to defaults.
 */
public class PreferencesService {

    /** Immutable preference snapshot. */
    public record Prefs(boolean darkMode, String defaultClass, String defaultSource) {
    }

    private static final String KEY_DARK = "ui.darkMode";
    private static final String KEY_CLASS = "travel.defaultClass";
    private static final String KEY_SOURCE = "travel.defaultSource";

    private final Path file;

    public PreferencesService() {
        this(Path.of(System.getProperty("user.home"), ".railreserve", "preferences.properties"));
    }

    /** Test seam: point at a temp file. */
    public PreferencesService(Path file) {
        this.file = file;
    }

    /** Loads preferences; never throws — bad/missing files yield defaults. */
    public Prefs load() {
        Properties props = new Properties();
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                props.load(reader);
            } catch (IOException | RuntimeException ignored) {
                // unreadable/corrupt: silently use defaults
            }
        }
        boolean dark = Boolean.parseBoolean(props.getProperty(KEY_DARK, "false"));
        String classType = sanitizeClass(props.getProperty(KEY_CLASS));
        String source = props.getProperty(KEY_SOURCE, "");
        return new Prefs(dark, classType, source == null ? "" : source.strip());
    }

    /** Persists preferences, creating the parent directory when needed. */
    public void save(Prefs prefs) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties props = new Properties();
        props.setProperty(KEY_DARK, String.valueOf(prefs.darkMode()));
        props.setProperty(KEY_CLASS, sanitizeClass(prefs.defaultClass()));
        props.setProperty(KEY_SOURCE, prefs.defaultSource() == null ? "" : prefs.defaultSource().strip());
        try (Writer writer = Files.newBufferedWriter(file)) {
            props.store(writer, "RailReserve UI preferences (safe to delete)");
        }
    }

    /** Ensures the class is one the booking form offers; falls back to the first. */
    public static String sanitizeClass(String classType) {
        if (classType != null) {
            for (String known : ReservationService.CLASS_TYPES) {
                if (known.equals(classType)) {
                    return known;
                }
            }
        }
        return ReservationService.CLASS_TYPES[0];
    }
}
