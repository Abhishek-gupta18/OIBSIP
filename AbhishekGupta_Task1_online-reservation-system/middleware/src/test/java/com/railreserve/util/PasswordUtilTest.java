package com.railreserve.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void hashProducesStorableFormat() {
        String stored = PasswordUtil.hash("demo123");
        assertTrue(stored.startsWith("pbkdf2$120000$"), "stored hash should be pbkdf2 format: " + stored);
        assertEquals(4, stored.split("\\$").length);
    }

    @Test
    void verifyAcceptsCorrectPassword() {
        String stored = PasswordUtil.hash("demo123");
        assertTrue(PasswordUtil.verify("demo123", stored));
    }

    @Test
    void verifyRejectsWrongPassword() {
        String stored = PasswordUtil.hash("demo123");
        assertFalse(PasswordUtil.verify("wrong", stored));
        assertFalse(PasswordUtil.verify("", stored));
        assertFalse(PasswordUtil.verify(null, stored));
    }

    @Test
    void samePasswordYieldsDifferentSaltsAndHashes() {
        String a = PasswordUtil.hash("demo123");
        String b = PasswordUtil.hash("demo123");
        assertNotEquals(a, b, "salts must be random so identical passwords differ");
        assertTrue(PasswordUtil.verify("demo123", a));
        assertTrue(PasswordUtil.verify("demo123", b));
    }

    @Test
    void verifyRejectsGarbageStoredValue() {
        assertFalse(PasswordUtil.verify("demo123", "plaintext-garbage"));
    }

    @Test
    void hashRejectsEmptyAndPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Special@Chars!2026", "my☺Passw0rd!", "Str0ng#Pass!2026", "aQ83_jK/#2"})
    void verifySupportsSpecialCharacterPasswords(String password) {
        assertTrue(PasswordUtil.verify(password, PasswordUtil.hash(password)));
    }
}
