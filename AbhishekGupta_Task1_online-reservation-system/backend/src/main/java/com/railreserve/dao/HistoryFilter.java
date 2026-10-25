package com.railreserve.dao;

import com.railreserve.model.Reservation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * WHERE-clause builder for the "My Bookings" table.
 *
 * SQL uses only whitelisted columns; values go through PreparedStatement
 * parameters, so this class is safe against injection and easy to unit-test
 * (it produces the SQL fragment plus the ordered parameter values).
 */
public final class HistoryFilter {

    private String status;           // null/blank = ALL, else CONFIRMED | CANCELLED
    private LocalDate journeyDate;   // null = any date
    private Integer trainNumber;     // null = any train
    private String pnr;              // null/blank = any PNR

    public HistoryFilter status(String status) {
        this.status = (status == null || status.isBlank()) ? null : status.strip().toUpperCase();
        return this;
    }

    public HistoryFilter journeyDate(LocalDate journeyDate) {
        this.journeyDate = journeyDate;
        return this;
    }

    public HistoryFilter trainNumber(Integer trainNumber) {
        this.trainNumber = trainNumber;
        return this;
    }

    public HistoryFilter pnr(String pnr) {
        this.pnr = (pnr == null || pnr.isBlank()) ? null : pnr.strip().toUpperCase();
        return this;
    }

    /** Builds " AND col = ?" fragments; every value becomes a PreparedStatement parameter. */
    public String toSql() {
        StringBuilder sql = new StringBuilder();
        if (status != null) {
            sql.append(" AND r.status = ?");
        }
        if (journeyDate != null) {
            sql.append(" AND r.journey_date = ?");
        }
        if (trainNumber != null) {
            sql.append(" AND r.train_number = ?");
        }
        if (pnr != null) {
            sql.append(" AND r.pnr = ?");
        }
        return sql.toString();
    }

    /** Ordered parameter values matching the fragments from toSql(). */
    public List<Object> parameters() {
        List<Object> params = new ArrayList<>();
        if (status != null) {
            params.add(status);
        }
        if (journeyDate != null) {
            params.add(journeyDate);
        }
        if (trainNumber != null) {
            params.add(trainNumber);
        }
        if (pnr != null) {
            params.add(pnr);
        }
        return params;
    }

    /** Only the two statuses supported by the schema are accepted; anything else = all. */
    public static boolean isKnownStatus(String status) {
        return status == null || status.isBlank()
                || Reservation.STATUS_CONFIRMED.equalsIgnoreCase(status.strip())
                || Reservation.STATUS_CANCELLED.equalsIgnoreCase(status.strip());
    }
}
