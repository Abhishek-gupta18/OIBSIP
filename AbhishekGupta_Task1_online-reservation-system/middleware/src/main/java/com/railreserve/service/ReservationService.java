package com.railreserve.service;

import com.railreserve.dao.ReservationDAO;
import com.railreserve.model.Reservation;
import com.railreserve.util.PnrGenerator;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Booking business rules: field validation, PNR generation and uniqueness
 * retry on duplicate PNR (SQLState 23505), retrieval and cancellation.
 * Kept free of Swing so it is fully unit-testable.
 */
public class ReservationService {

    /** Duplicate-PNR SQLState on PostgreSQL. On this, we regenerate and retry. */
    static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

    public static final String[] CLASS_TYPES = {
            "Sleeper (SL)", "AC 3 Tier (3A)", "AC 2 Tier (2A)", "First AC (1A)", "Chair Car (CC)"
    };

    private final ReservationDAO reservationDAO;

    /** Seam for tests so bookings can be faked without a database. */
    @FunctionalInterface
    public interface PnrSupplier {
        String get();
    }

    private final PnrSupplier pnrSupplier;

    public ReservationService() {
        this(new ReservationDAO(), PnrGenerator::next);
    }

    /** Constructor used by unit tests to inject fakes. */
    public ReservationService(ReservationDAO reservationDAO, PnrSupplier pnrSupplier) {
        this.reservationDAO = reservationDAO;
        this.pnrSupplier = pnrSupplier;
    }

    // ---------- Validation ----------

    /** Validates booking form data; returns null when it is all good. */
    public String validate(String passengerName, String trainNumber, String trainName,
                           String classType, LocalDate journeyDate,
                           String source, String destination) {
        if (passengerName == null || passengerName.isBlank()) {
            return "Passenger name is required.";
        }
        if (trainNumber == null || trainNumber.isBlank()) {
            return "Train number is required.";
        }
        if (!trainNumber.strip().matches("\\d{3,6}")) {
            return "Train number must be numeric (3–6 digits).";
        }
        if (trainName == null || trainName.isBlank()) {
            return "Train name could not be resolved — use a valid train number.";
        }
        if (classType == null || classType.isBlank()) {
            return "Class type is required.";
        }
        if (journeyDate == null) {
            return "Journey date is required.";
        }
        if (journeyDate.isBefore(LocalDate.now())) {
            return "Journey date cannot be in the past.";
        }
        if (source == null || source.isBlank()) {
            return "Source station is required.";
        }
        if (destination == null || destination.isBlank()) {
            return "Destination station is required.";
        }
        if (source.strip().equalsIgnoreCase(destination.strip())) {
            return "Source and destination stations must be different.";
        }
        return null;
    }

    // ---------- Booking ----------

    /** Saves the booking, retrying with a fresh PNR when a duplicate occurs. */
    public BookingResult book(int userId, String passengerName, String trainNumberStr,
                              String trainName, String classType, LocalDate journeyDate,
                              String source, String destination) {
        String error = validate(passengerName, trainNumberStr, trainName,
                classType, journeyDate, source, destination);
        if (error != null) {
            return BookingResult.fail(error);
        }

        Reservation candidate = new Reservation(
                pnrSupplier.get(),
                userId,
                passengerName.strip(),
                Integer.parseInt(trainNumberStr.strip()),
                trainName.strip(),
                classType.strip(),
                journeyDate,
                source.strip(),
                destination.strip(),
                Reservation.STATUS_CONFIRMED,
                null);

        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                Reservation saved = reservationDAO.insert(candidate);
                return BookingResult.ok(saved);
            } catch (SQLException e) {
                if (UNIQUE_VIOLATION_SQLSTATE.equals(e.getSQLState())) {
                    candidate = withFreshPnr(candidate);
                    continue; // PNR collision — regenerate and retry
                }
                return BookingResult.fail("Could not save the booking right now. Please check the connection and try again.");
            }
        }
        return BookingResult.fail("Could not generate a unique PNR after several attempts. Please retry.");
    }

    private Reservation withFreshPnr(Reservation r) {
        return new Reservation(pnrSupplier.get(), r.getUserId(), r.getPassengerName(),
                r.getTrainNumber(), r.getTrainName(), r.getClassType(), r.getJourneyDate(),
                r.getSourceStation(), r.getDestinationStation(), r.getStatus(), r.getCreatedAt());
    }

    // ---------- Retrieval + cancellation ----------

    /** Looks a PNR up; empty result yields a friendly not-found message. */
    public Optional<Reservation> findByPnr(String pnr) throws SQLException {
        if (pnr == null || pnr.isBlank()) {
            return Optional.empty();
        }
        return reservationDAO.findByPnr(pnr.trim().toUpperCase());
    }

    /** Cancels a booking; returns an error message on failure or already-cancelled PNRs. */
    public String cancel(String pnr) {
        try {
            Optional<Reservation> found = findByPnr(pnr);
            if (found.isEmpty()) {
                return "No booking found for PNR " + pnr + ".";
            }
            boolean cancelled = reservationDAO.cancel(found.get().getPnr());
            return cancelled ? null : "This booking is already CANCELLED.";
        } catch (SQLException e) {
            return "Could not cancel the booking right now. Please try again shortly.";
        }
    }

    /** Booking history of a user (optional feature). */
    public List<Reservation> historyFor(int userId) throws SQLException {
        return reservationDAO.findByUser(userId);
    }

    /**
     * My Bookings query: one user's bookings filtered on the SQL side.
     * status null/blank = ALL; only CONFIRMED/CANCELLED filters are accepted —
     * anything else is silently treated as ALL (known-status whitelist).
     */
    public List<Reservation> historyFiltered(int userId, String status,
                                             LocalDate journeyDate, Integer trainNumber, String pnr)
            throws SQLException {
        return reservationDAO.findByUserFiltered(userId,
                new com.railreserve.dao.HistoryFilter()
                        .status(com.railreserve.dao.HistoryFilter.isKnownStatus(status) ? status : null)
                        .journeyDate(journeyDate)
                        .trainNumber(trainNumber)
                        .pnr(pnr));
    }

    // ---------- Result types ----------

    public record BookingResult(boolean success, Reservation reservation, String errorMessage) {

        public static BookingResult ok(Reservation r) {
            return new BookingResult(true, r, null);
        }

        public static BookingResult fail(String message) {
            return new BookingResult(false, null, message);
        }
    }
}
