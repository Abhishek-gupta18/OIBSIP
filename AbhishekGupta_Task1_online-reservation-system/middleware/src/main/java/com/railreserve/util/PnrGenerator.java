package com.railreserve.util;

import java.security.SecureRandom;

/**
 * Generates unique 10-character PNRs: uppercase letters and digits,
 * always starting with a letter, e.g. "R3K7A9BD2X".
 */
public final class PnrGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no I, O, 0, 1
    private static final SecureRandom RANDOM = new SecureRandom();
    static final int LENGTH = 10;

    private PnrGenerator() {
    }

    /** New random PNR. Uniqueness is guaranteed by the caller retrying on collisions. */
    public static String next() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
