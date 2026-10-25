package com.railreserve.service;

import com.railreserve.dao.ReservationDAO;
import com.railreserve.model.Reservation;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationServiceBookingTest {

    /** In-memory DAO fake: no database required, counts duplicate-PNR events. */
    static class FakeReservationDAO extends ReservationDAO {
        final Map<String, Reservation> store = new HashMap<>();
        final List<String> attemptedPnrs = new ArrayList<>();
        int duplicatePnrFailuresLeft;

        @Override
        public Reservation insert(Reservation r) throws SQLException {
            attemptedPnrs.add(r.getPnr());
            if (duplicatePnrFailuresLeft > 0) {
                duplicatePnrFailuresLeft--;
                throw new SQLException("duplicate key value violates unique constraint",
                        ReservationService.UNIQUE_VIOLATION_SQLSTATE);
            }
            if (store.containsKey(r.getPnr())) {
                throw new SQLException("duplicate key value violates unique constraint",
                        ReservationService.UNIQUE_VIOLATION_SQLSTATE);
            }
            store.put(r.getPnr(), r);
            return r;
        }

        @Override
        public Optional<Reservation> findByPnr(String pnr) {
            return Optional.ofNullable(store.get(pnr));
        }

        @Override
        public boolean cancel(String pnr) {
            Reservation r = store.get(pnr);
            if (r == null || r.isCancelled()) {
                return false;
            }
            store.put(pnr, cancelledCopy(r));
            return true;
        }

        private static Reservation cancelledCopy(Reservation r) {
            return new Reservation(r.getPnr(), r.getUserId(), r.getPassengerName(),
                    r.getTrainNumber(), r.getTrainName(), r.getClassType(), r.getJourneyDate(),
                    r.getSourceStation(), r.getDestinationStation(),
                    Reservation.STATUS_CANCELLED, r.getCreatedAt());
        }
    }

    private static ReservationService serviceFor(FakeReservationDAO dao, String... pnrSequence) {
        if (pnrSequence.length == 0) {
            java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
            return new ReservationService(dao,
                    () -> String.format("TX%07dR", counter.incrementAndGet()));
        }
        return new ReservationService(dao, new java.util.ArrayDeque<>(List.of(pnrSequence))::poll);
    }

    private static final LocalDate FUTURE = LocalDate.now().plusDays(3);

    @Test
    void booksAValidReservationWithUniquePnr() {
        FakeReservationDAO dao = new FakeReservationDAO();
        ReservationService service = serviceFor(dao);

        ReservationService.BookingResult result = service.book(1, "Asha Verma", "12951",
                "Mumbai Rajdhani Express", "AC 3 Tier (3A)", FUTURE, "Mumbai", "Delhi");

        assertTrue(result.success(), "booking should succeed");
        assertNotNull(result.reservation().getPnr());
        assertEquals(10, result.reservation().getPnr().length());
        assertEquals("CONFIRMED", result.reservation().getStatus());
        assertEquals(1, dao.store.size(), "booking must be saved exactly once");
        assertTrue(dao.store.containsKey(result.reservation().getPnr()));
    }

    @Test
    void retryRegeneratesPnrOnDuplicateKeyCollision() {
        FakeReservationDAO dao = new FakeReservationDAO();
        dao.duplicatePnrFailuresLeft = 1; // first insert collides with an existing PNR
        ReservationService service = serviceFor(dao, "FIRSTPNR00", "SECONDNR00");

        ReservationService.BookingResult result = service.book(1, "Asha Verma", "12951",
                "Mumbai Rajdhani Express", "SL", FUTURE, "Mumbai", "Delhi");

        assertTrue(result.success(), "booking must succeed after retry");
        assertEquals("SECONDNR00", result.reservation().getPnr(),
                "collided PNR must be replaced by a regenerated one");
        assertNotEquals("FIRSTPNR00", result.reservation().getPnr());
        assertEquals(2, dao.attemptedPnrs.size(), "two insert attempts expected");
        assertEquals(1, dao.store.size());
    }

    @Test
    void bookingFailsCleanlyWhenEveryGeneratedPnrCollides() {
        FakeReservationDAO dao = new FakeReservationDAO();
        // Every generated PNR collides with an existing booking in the store
        Reservation existing = new Reservation("BUSYPNR000", 9, "Someone", 12951, "Rajdhani",
                "SL", FUTURE, "A", "B", "CONFIRMED", null);
        dao.store.put("BUSYPNR000", existing);
        ReservationService service = serviceFor(dao, "BUSYPNR000", "BUSYPNR000", "BUSYPNR000",
                "BUSYPNR000", "BUSYPNR000");

        ReservationService.BookingResult result = service.book(1, "Asha Verma", "12951",
                "Rajdhani", "SL", FUTURE, "A", "B");

        assertTrue(!result.success(), "permanent collisions must surface as a friendly failure");
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("unique PNR"),
                "message should explain the retry budget ran out");
        assertEquals(1, dao.store.size(), "no second booking may appear");
    }

    @Test
    void invalidDataReturnsFriendlyErrorAndDoesNotTouchDatabase() {
        FakeReservationDAO dao = new FakeReservationDAO();
        ReservationService service = serviceFor(dao);

        ReservationService.BookingResult result = service.book(1, "   ", "12951",
                "Rajdhani", "SL", FUTURE, "A", "A");

        assertTrue(!result.success(), "blank name + same stations must fail");
        assertNotNull(result.errorMessage());
        assertEquals(0, dao.store.size(), "database must not be touched on validation failure");
        assertEquals(0, dao.attemptedPnrs.size(), "no PNR should even be generated");
    }

    @Test
    void cancelledBookingIsMarkedCancelledAndPersists() {
        FakeReservationDAO dao = new FakeReservationDAO();
        ReservationService service = serviceFor(dao, "RIDEHOME01");
        ReservationService.BookingResult booked = service.book(1, "Asha Verma", "12951",
                "Rajdhani", "SL", FUTURE, "Mumbai", "Delhi");
        String pnr = booked.reservation().getPnr();

        String error = service.cancel(pnr);

        assertNull(error, "cancellation should succeed");
        assertEquals("CANCELLED", dao.store.get(pnr).getStatus(),
                "status must be persisted as CANCELLED");
        assertEquals("CONFIRMED", booked.reservation().getStatus(),
                "original in-memory object untouched (fresh read shows new status)");
    }

    @Test
    void cancellingAnUnknownPnrReturnsFriendlyMessage() {
        FakeReservationDAO dao = new FakeReservationDAO();
        ReservationService service = serviceFor(dao);

        String error = service.cancel("NOPE99PNR");

        assertNotNull(error);
        assertTrue(error.contains("No booking found"));
    }

    @Test
    void cancellingAnAlreadyCancelledBookingIsRejected() {
        FakeReservationDAO dao = new FakeReservationDAO();
        ReservationService service = serviceFor(dao, "GONEISGONE");
        String pnr = service.book(1, "Asha Verma", "12951", "Rajdhani", "SL",
                FUTURE, "Mumbai", "Delhi").reservation().getPnr();
        assertNull(service.cancel(pnr));
        assertEquals("CANCELLED", dao.store.get(pnr).getStatus());

        String secondTry = service.cancel(pnr);

        assertNotNull(secondTry, "second cancellation must be rejected");
        assertTrue(secondTry.contains("already CANCELLED"));
    }
}
