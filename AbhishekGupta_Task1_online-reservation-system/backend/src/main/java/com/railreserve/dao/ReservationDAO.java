package com.railreserve.dao;

import com.railreserve.config.DatabaseConfig;
import com.railreserve.model.Reservation;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC data access for the reservations table. All SQL uses PreparedStatement. */
public class ReservationDAO {

    /** Column list shared by all row-mapping queries. */
    private static final String SELECT_COLUMNS = """
            SELECT r.pnr, r.user_id, r.passenger_name, r.train_number, r.class_type,
                   r.journey_date, r.source_station, r.destination_station, r.status,
                   r.created_at, t.train_name
            FROM reservations r
            JOIN trains t ON t.train_number = r.train_number
            """;

    /** Inserts one booking with the given PNR. Throws SQLException on duplicates etc. */
    public Reservation insert(Reservation r) throws SQLException {
        String sql = """
                INSERT INTO reservations (pnr, user_id, passenger_name, train_number, class_type,
                                          journey_date, source_station, destination_station, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, r.getPnr());
            ps.setInt(2, r.getUserId());
            ps.setString(3, r.getPassengerName());
            ps.setInt(4, r.getTrainNumber());
            ps.setString(5, r.getClassType());
            ps.setDate(6, Date.valueOf(r.getJourneyDate()));
            ps.setString(7, r.getSourceStation());
            ps.setString(8, r.getDestinationStation());
            ps.setString(9, r.getStatus());
            ps.executeUpdate();
        }
        return r;
    }

    public Optional<Reservation> findByPnr(String pnr) throws SQLException {
        String sql = """
                SELECT r.pnr, r.user_id, r.passenger_name, r.train_number, r.class_type,
                       r.journey_date, r.source_station, r.destination_station, r.status, r.created_at,
                       t.train_name
                FROM reservations r
                JOIN trains t ON t.train_number = r.train_number
                WHERE r.pnr = ?
                """;
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /** All bookings of one user, newest first (optional booking-history view). */
    public List<Reservation> findByUser(int userId) throws SQLException {
        String sql = """
                SELECT r.pnr, r.user_id, r.passenger_name, r.train_number, r.class_type,
                       r.journey_date, r.source_station, r.destination_station, r.status, r.created_at,
                       t.train_name
                FROM reservations r
                JOIN trains t ON t.train_number = r.train_number
                WHERE r.user_id = ?
                ORDER BY r.created_at DESC
                """;
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** Bookings of one user, optionally filtered (SQL-side via HistoryFilter), newest first. */
    public List<Reservation> findByUserFiltered(int userId, com.railreserve.dao.HistoryFilter filter)
            throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE r.user_id = ?" + filter.toSql()
                + " ORDER BY r.created_at DESC";
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(filter.parameters());
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object value = params.get(i);
                if (value instanceof LocalDate d) {
                    ps.setDate(i + 1, Date.valueOf(d));
                } else if (value instanceof Integer n) {
                    ps.setInt(i + 1, n);
                } else {
                    ps.setString(i + 1, String.valueOf(value));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** Marks a booking CANCELLED; returns true when a row was actually updated. */
    public boolean cancel(String pnr) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE pnr = ? AND status = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, Reservation.STATUS_CANCELLED);
            ps.setString(2, pnr);
            ps.setString(3, Reservation.STATUS_CONFIRMED);
            return ps.executeUpdate() == 1;
        }
    }

    /** True when the PNR already exists (used to avoid 23505 exceptions in tests). */
    public boolean exists(String pnr) throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE pnr = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Reservation mapRow(ResultSet rs) throws SQLException {
        Date d = rs.getDate("journey_date");
        LocalDate journeyDate = d == null ? null : d.toLocalDate();
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts == null ? null : ts.toLocalDateTime();
        return new Reservation(
                rs.getString("pnr"),
                rs.getInt("user_id"),
                rs.getString("passenger_name"),
                rs.getInt("train_number"),
                rs.getString("train_name"),
                rs.getString("class_type"),
                journeyDate,
                rs.getString("source_station"),
                rs.getString("destination_station"),
                rs.getString("status"),
                createdAt);
    }
}
