package com.railreserve.ui;

import java.time.LocalDate;

/** A hero-search filled before login; replayed into the booking form after login. */
public record SearchIntent(String fromDisplay, String toDisplay,
                           LocalDate journeyDate, String classType) {
}
