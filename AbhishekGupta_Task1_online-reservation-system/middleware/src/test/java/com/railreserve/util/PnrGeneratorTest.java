package com.railreserve.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PnrGeneratorTest {

    private static final int SAMPLE_SIZE = 100_000;
    private static final Pattern FORMAT = Pattern.compile("[A-HJ-NP-Z2-9]{10}");

    @Test
    void pnrIsTenCharacters() {
        for (int i = 0; i < 1_000; i++) {
            assertEquals(10, PnrGenerator.next().length(), "PNR must be exactly 10 characters");
        }
    }

    @Test
    void pnrCharsetIsUnambiguousUpperAlphanumeric() {
        for (int i = 0; i < 1_000; i++) {
            assertTrue(FORMAT.matcher(PnrGenerator.next()).matches(),
                    "PNR must be uppercase letters/digits without I,O,0,1");
        }
    }

    @Test
    void pnrGenerationIsUniqueAcrossLargeSample() {
        Set<String> unique = new HashSet<>();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            assertTrue(unique.add(PnrGenerator.next()),
                    "duplicate PNR produced within " + SAMPLE_SIZE + " samples");
        }
        assertEquals(SAMPLE_SIZE, unique.size());
    }
}
