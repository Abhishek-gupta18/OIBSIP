package com.railreserve.dao;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryFilterTest {

    @Test
    void emptyFilterProducesNoWhereFragmentAndNoParameters() {
        HistoryFilter filter = new HistoryFilter();
        assertEquals("", filter.toSql());
        assertEquals(List.of(), filter.parameters());
    }

    @Test
    void blankInputsCountAsNoFilter() {
        HistoryFilter filter = new HistoryFilter()
                .status("   ")
                .pnr("  ");
        assertEquals("", filter.toSql());
        assertEquals(List.of(), filter.parameters());
    }

    @Test
    void statusFilterNormalizesToUppercase() {
        HistoryFilter filter = new HistoryFilter().status("confirmed");
        assertEquals(" AND r.status = ?", filter.toSql());
        assertEquals(List.of("CONFIRMED"), filter.parameters());
    }

    @Test
    void pnrFilterNormalizesToUppercase() {
        HistoryFilter filter = new HistoryFilter().pnr(" r3k7a9bd2x ");
        assertEquals(" AND r.pnr = ?", filter.toSql());
        assertEquals(List.of("R3K7A9BD2X"), filter.parameters());
    }

    @Test
    void allFiltersProduceOrderedPlaceholderAndParameterList() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        HistoryFilter filter = new HistoryFilter()
                .status("CANCELLED")
                .journeyDate(date)
                .trainNumber(12951)
                .pnr("R3K7A9BD2X");

        assertEquals(" AND r.status = ? AND r.journey_date = ? AND r.train_number = ? AND r.pnr = ?",
                filter.toSql());
        List<Object> params = filter.parameters();
        assertEquals(4, params.size());
        assertEquals("CANCELLED", params.get(0));
        assertEquals(date, params.get(1));
        assertEquals(12951, params.get(2));
        assertEquals("R3K7A9BD2X", params.get(3));
    }

    @Test
    void fragmentsMatchParameterPositionsInSqlOrder() {
        // Selective combinations must keep fragment order aligned with params order
        HistoryFilter filter = new HistoryFilter()
                .journeyDate(LocalDate.of(2026, 11, 5))
                .trainNumber(12951);
        String sql = filter.toSql();
        assertTrue(sql.contains("journey_date = ?"));
        assertTrue(sql.indexOf("journey_date") < sql.indexOf("train_number"));
        assertEquals(2, filter.parameters().size());
    }

    @Test
    void knownStatusWhitelist() {
        assertTrue(HistoryFilter.isKnownStatus(null));
        assertTrue(HistoryFilter.isKnownStatus("   "));
        assertTrue(HistoryFilter.isKnownStatus("confirmed"));
        assertTrue(HistoryFilter.isKnownStatus("Cancelled"));
        // "ALL" is not a database status; the service sanitizes it to "no filter",
        // as asserted in ReservationServiceHistoryTest.emptyFilterSendsNoConditions
        assertTrue(!HistoryFilter.isKnownStatus("ALL"));
        assertTrue(!HistoryFilter.isKnownStatus("DROP TABLE reservations"));
    }
}
