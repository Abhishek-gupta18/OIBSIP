package com.railreserve.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReservationServiceValidationTest {

    private final ReservationService service = new ReservationService(
            new com.railreserve.dao.ReservationDAO(), () -> "PNRTEST000");

    private static final LocalDate FUTURE = LocalDate.now().plusDays(7);
    private static final LocalDate NOW = LocalDate.now();
    private static final LocalDate PAST = LocalDate.now().minusDays(1);

    @Test
    void acceptsAValidBooking() {
        assertNull(service.validate("Asha Verma", "12951", "Mumbai Rajdhani Express",
                "AC 3 Tier (3A)", FUTURE, "Mumbai", "Delhi"));
        // Today is allowed: valid and not in the past
        assertNull(service.validate("Asha Verma", "12951", "Mumbai Rajdhani Express",
                "AC 3 Tier (3A)", NOW, "Mumbai", "Delhi"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankPassengerName(String name) {
        assertNotNull(service.validate(name, "12951", "Rajdhani", "SL", FUTURE, "Mumbai", "Delhi"),
                "blank passenger name must be rejected");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12A5", "12", "12345678901", "12951.0"})
    void rejectsNonNumericTrainNumbers(String trainNumber) {
        assertNotNull(service.validate("Asha", trainNumber, "Rajdhani", "SL", FUTURE, "A", "B"),
                "non-numeric train number must be rejected: " + trainNumber);
    }

    @Test
    void rejectsPastJourneyDate() {
        assertNotNull(service.validate("Asha", "12951", "Rajdhani", "SL", PAST, "A", "B"),
                "past journey date must be rejected");
    }

    @Test
    void rejectsMissingTrainName() {
        assertNotNull(service.validate("Asha", "12951", "", "SL", FUTURE, "A", "B"));
        assertNotNull(service.validate("Asha", "12951", null, "SL", FUTURE, "A", "B"));
    }

    @Test
    void rejectsMissingClassType() {
        assertNotNull(service.validate("Asha", "12951", "Rajdhani", null, FUTURE, "A", "B"));
        assertNotNull(service.validate("Asha", "12951", "Rajdhani", "  ", FUTURE, "A", "B"));
    }

    @Test
    void rejectsNullJourneyDate() {
        assertNotNull(service.validate("Asha", "12951", "Rajdhani", "SL", null, "A", "B"));
    }

    @Test
    void rejectsBlankStations() {
        assertNotNull(service.validate("Asha", "12951", "Rajdhani", "SL", FUTURE, "   ", "B"));
        assertNotNull(service.validate("Asha", "12951", "Rajdhani", "SL", FUTURE, "A", null));
    }

    @Test
    void rejectsSameSourceAndDestination() {
        assertNotNull(service.validate("Asha", "12951", "Rajdhani", "SL", FUTURE, "Mumbai", "mumbai"),
                "source == destination (case-insensitive) must be rejected");
        assertNotNull(service.validate("Asha", "12951", "Rajdhani", "SL", FUTURE, "Mumbai", "Mumbai"));
    }
}
