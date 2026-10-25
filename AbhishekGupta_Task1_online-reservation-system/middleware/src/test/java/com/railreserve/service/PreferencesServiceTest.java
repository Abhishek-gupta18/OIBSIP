package com.railreserve.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Local preferences file: round-trip, defaults and validation. */
class PreferencesServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void missingFileYieldsSensibleDefaults() {
        PreferencesService service = new PreferencesService(tempDir.resolve("nothing-here.properties"));

        PreferencesService.Prefs prefs = service.load();

        assertFalse(prefs.darkMode());
        assertEquals(ReservationService.CLASS_TYPES[0], prefs.defaultClass());
        assertEquals("", prefs.defaultSource());
    }

    @Test
    void saveThenLoadRoundTrips() throws IOException {
        PreferencesService service = new PreferencesService(tempDir.resolve("nested/prefs.properties"));

        service.save(new PreferencesService.Prefs(true, "AC 2 Tier (2A)", "New Delhi"));

        PreferencesService.Prefs prefs = service.load();
        assertTrue(service.load().darkMode());
        assertEquals("AC 2 Tier (2A)", prefs.defaultClass());
        assertEquals("New Delhi", prefs.defaultSource());
    }

    @Test
    void unknownClassIsSanitizedOnSaveAndLoad() throws IOException {
        Path file = tempDir.resolve("prefs.properties");
        Files.writeString(file, "travel.defaultClass=Hyperloop Deluxe\n");
        PreferencesService service = new PreferencesService(file);

        assertEquals(ReservationService.CLASS_TYPES[0], service.load().defaultClass());
        assertEquals(ReservationService.CLASS_TYPES[1],
                PreferencesService.sanitizeClass(ReservationService.CLASS_TYPES[1]));
    }

    @Test
    void corruptFileDoesNotThrow() throws IOException {
        Path file = tempDir.resolve("broken.properties");
        Files.writeString(file, "\u0000\u0001 not a properties file \\");
        PreferencesService service = new PreferencesService(file);

        assertEquals(ReservationService.CLASS_TYPES[0], service.load().defaultClass());
    }
}
