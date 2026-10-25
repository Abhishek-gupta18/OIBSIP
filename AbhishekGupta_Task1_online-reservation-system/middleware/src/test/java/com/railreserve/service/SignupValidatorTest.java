package com.railreserve.service;

import com.railreserve.service.SignupValidator.Strength;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rules for name, username, email, phone, password policy and strength. */
class SignupValidatorTest {

    @Test
    void fullNameIsRequired() {
        assertNotNull(SignupValidator.fullNameError(null));
        assertNotNull(SignupValidator.fullNameError("   "));
        assertNull(SignupValidator.fullNameError("Abhishek Gupta"));
    }

    @Test
    void usernameFollowsLengthAndCharsetRules() {
        assertNull(SignupValidator.usernameError("abhi_99"));
        assertNull(SignupValidator.usernameError("USER"));
        assertNotNull(SignupValidator.usernameError("abc"));          // too short
        assertNotNull(SignupValidator.usernameError("has space"));    // charset
        assertNotNull(SignupValidator.usernameError("way_toolongname20+")); // >20
    }

    @Test
    void emailMustLookLikeEmail() {
        assertNull(SignupValidator.emailError("me@example.com"));
        assertNull(SignupValidator.emailError("a.b+c@sub.domain.org"));
        assertNotNull(SignupValidator.emailError("no-at-sign"));
        assertNotNull(SignupValidator.emailError("a@b"));             // no tld
        assertNotNull(SignupValidator.emailError("a b@example.com")); // space
    }

    @Test
    void phoneOptionalYetShapeChecked() {
        assertNull(SignupValidator.phoneError(null));
        assertNull(SignupValidator.phoneError(""));
        assertNull(SignupValidator.phoneError("+91 98765 43210"));
        assertNull(SignupValidator.phoneError("9876543210"));
        assertNotNull(SignupValidator.phoneError("call me"));
        assertNotNull(SignupValidator.phoneError("123"));             // too short
    }

    @Test
    void passwordPolicyNeedsUpperLowerDigitAndLength() {
        assertNull(SignupValidator.passwordError("Sup3rSafe"));
        assertNotNull(SignupValidator.passwordError(null));
        assertNotNull(SignupValidator.passwordError("Sh0rt"));        // < 8
        assertNotNull(SignupValidator.passwordError("alllower1"));    // no upper
        assertNotNull(SignupValidator.passwordError("ALLUPPER1"));    // no lower
        assertNotNull(SignupValidator.passwordError("NoDigitsHere")); // no digit
    }

    @Test
    void confirmMustMatchExactly() {
        assertNull(SignupValidator.mismatchError("Sup3rSafe", "Sup3rSafe"));
        assertNotNull(SignupValidator.mismatchError("Sup3rSafe", "Sup3rSafe1"));
        assertNotNull(SignupValidator.mismatchError("Sup3rSafe", ""));
        assertNotNull(SignupValidator.mismatchError("Sup3rSafe", null));
    }

    @Test
    void wholeFormErrorsCollectPerField() {
        SignupValidator.FieldErrors errors = SignupValidator.validate(
                "", "bad name", "x", "123", "weak", "notsame");
        assertTrue(errors.hasAny());
        assertEquals("Full name is required.", errors.fullName());
        assertNotNull(errors.username());
        assertNotNull(errors.email());
        assertNotNull(errors.phone());
        assertNotNull(errors.password());
        assertNotNull(errors.confirm());

        SignupValidator.FieldErrors clean = SignupValidator.validate(
                "Abhishek", "abhi", "a@b.com", "", "Sup3rSafe", "Sup3rSafe");
        assertNull(clean.fullName());
        assertNull(clean.username());
        assertNull(clean.email());
        assertNull(clean.phone());
        assertNull(clean.password());
        assertNull(clean.confirm());
        assertTrue(!clean.hasAny(), "clean form must carry no errors");
    }

    @Test
    void strengthEscalatesWithLengthAndVariety() {
        assertEquals(Strength.WEAK, SignupValidator.strength(null));
        assertEquals(Strength.WEAK, SignupValidator.strength("abc"));
        assertEquals(Strength.STRONG, SignupValidator.strength("Sup3r$afePassw0rd"));
        assertEquals(Strength.WEAK, SignupValidator.strength("aaaaaaaa8"));   // length but one variety
        assertTrue(SignupValidator.strength("Fair1Way") != Strength.WEAK);
    }
}
