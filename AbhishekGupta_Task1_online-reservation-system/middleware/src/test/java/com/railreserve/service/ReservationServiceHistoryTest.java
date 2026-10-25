package com.railreserve.service;

import com.railreserve.dao.ReservationDAO;
import com.railreserve.model.Reservation;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationServiceHistoryTest {

    /** Records the filter the service builds, without touching any database. */
    static class RecordingFakeDAO extends ReservationDAO {
        final AtomicReference<String> recordedSql = new AtomicReference<>("");
        final AtomicReference<java.util.List<Object>> recordedParams = new AtomicReference<>(List.of());

        @Override
        public List<Reservation> findByUserFiltered(int userId, com.railreserve.dao.HistoryFilter filter)
                throws SQLException {
            recordedSql.set(filter.toSql());
            recordedParams.set(filter.parameters());
            return List.of();
        }

        @Override
        public Optional<Reservation> findByPnr(String pnr) {
            return Optional.empty();
        }
    }

    @Test
    void unknownStatusIsSanitizedToAllInsteadOfReachingSql() throws SQLException {
        RecordingFakeDAO dao = new RecordingFakeDAO();
        ReservationService service = new ReservationService(dao, () -> "PNR00001XX");

        String hostile = "CONFIRMED'; DROP TABLE reservations;--";
        List<Reservation> ignored = service.historyFiltered(1, hostile, null, null, null);

        assertTrue(ignored.isEmpty());
        // The hostile string must not appear in the SQL; unknown status => ALL (no fragment)
        assertEquals("", dao.recordedSql.get());
        assertTrue(dao.recordedParams.get().isEmpty());
    }

    @Test
    void knownStatusesArePassedThroughAsSqlParameters() throws SQLException {
        RecordingFakeDAO dao = new RecordingFakeDAO();
        ReservationService service = new ReservationService(dao, () -> "PNR00001XX");

        LocalDate date = LocalDate.of(2026, 11, 5);
        List<Reservation> ignored = service.historyFiltered(1, "cancelled", date, 12951, "r3k7a9bd2x");

        assertEquals(" AND r.status = ? AND r.journey_date = ? AND r.train_number = ? AND r.pnr = ?",
                dao.recordedSql.get());
        List<Object> params = dao.recordedParams.get();
        assertEquals(4, params.size());
        assertEquals("CANCELLED", params.get(0));
        assertEquals(date, params.get(1));
        assertEquals(12951, params.get(2));
        assertEquals("R3K7A9BD2X", params.get(3));
    }

    @Test
    void emptyFilterSendsNoConditions() throws SQLException {
        RecordingFakeDAO dao = new RecordingFakeDAO();
        ReservationService service = new ReservationService(dao, () -> "PNR00001XX");

        List<Reservation> ignored = service.historyFiltered(1, "ALL", null, null, "   ");

        assertEquals("", dao.recordedSql.get());
        assertTrue(dao.recordedParams.get().isEmpty());
        assertFalse(dao.recordedSql.get().contains(";"), "SQL must stay parameter-only, no text building");
    }
}
