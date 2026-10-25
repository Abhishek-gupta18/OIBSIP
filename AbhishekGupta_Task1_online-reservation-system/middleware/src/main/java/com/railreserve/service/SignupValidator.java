package com.railreserve.service;

import java.util.regex.Pattern;

/**
 * Pure sign-up field validation and password strength scoring. No DB access —
 * every rule is a deterministic static check so it is trivially unit-testable
 * and reusable by Settings > change-password (strength meter).
 */
public final class SignupValidator {

    /** Username: 4–20 chars, letters/digits/underscore. */
    private static final Pattern USERNAME = Pattern.compile("\\w{4,20}");
    /** Pragmatic e-mail check: local@domain.tld, no spaces. */
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private SignupValidator() {
    }

    /** One inline error per field; null = field is fine. */
    public record FieldErrors(String fullName, String username, String email,
                              String phone, String password, String confirm) {

        public boolean hasAny() {
            return fullName != null || username != null || email != null
                    || phone != null || password != null || confirm != null;
        }
    }

    public static FieldErrors validate(String fullName, String username, String email,
                                       String phone, String password, String confirm) {
        return new FieldErrors(
                fullNameError(fullName),
                usernameError(username),
                emailError(email),
                phoneError(phone),
                passwordError(password),
                mismatchError(password, confirm));
    }

    public static String fullNameError(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Full name is required.";
        }
        if (fullName.strip().length() > 100) {
            return "Full name is too long (max 100 chars).";
        }
        return null;
    }

    public static String usernameError(String username) {
        if (username == null || username.isBlank()) {
            return "Username is required.";
        }
        if (!USERNAME.matcher(username.strip()).matches()) {
            return "4–20 letters, digits or underscore only.";
        }
        return null;
    }

    public static String emailError(String email) {
        if (email == null || email.isBlank()) {
            return "E-mail is required.";
        }
        if (email.strip().length() > 120 || !EMAIL.matcher(email.strip()).matches()) {
            return "Enter a valid e-mail (name@example.com).";
        }
        return null;
    }

    /** Phone optional; when given must be 10 digits or +XX… up to 20 chars. */
    public static String phoneError(String phone) {
        if (phone == null || phone.isBlank()) {
            return null; // optional
        }
        String v = phone.strip();
        if (!v.matches("\\+?\\d[\\d- ]{7,19}")) {
            return "Use digits, spaces or dashes (e.g. +91 98765 43210).";
        }
        return null;
    }

    /** Password policy: min 8 chars with at least one upper, one lower, one digit. */
    public static String passwordError(String password) {
        if (password == null || password.length() < 8) {
            return "At least 8 characters.";
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            return "Add at least one UPPERCASE letter.";
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            return "Add at least one lowercase letter.";
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            return "Add at least one digit.";
        }
        return null;
    }

    public static String mismatchError(String password, String confirm) {
        if (confirm == null || confirm.isEmpty()) {
            return "Confirm your password.";
        }
        if (!confirm.equals(password)) {
            return "Passwords do not match.";
        }
        return null;
    }

    // ---------- strength meter ----------

    public enum Strength { WEAK, FAIR, GOOD, STRONG }

    /** Criteria-based score: length buckets + variety; never throws on null/empty. */
    public static Strength strength(String password) {
        if (password == null || password.isEmpty()) {
            return Strength.WEAK;
        }
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        int variety = (upper ? 1 : 0) + (lower ? 1 : 0) + (digit ? 1 : 0) + (special ? 1 : 0);
        score += switch (variety) {
            case 2 -> 1;
            default -> variety >= 3 ? 2 : 0;   // 3–4 character classes
        };
        if (score <= 2) return Strength.WEAK;
        if (score == 3) return Strength.FAIR;
        if (score == 4) return Strength.GOOD;
        return Strength.STRONG;   // score 5+: long AND varied
    }

    /** Short user-facing hint shown next to the meter. */
    public static String strengthLabel(Strength strength) {
        return switch (strength) {
            case WEAK -> "Weak — add uppercase, digits and more characters";
            case FAIR -> "Fair — could be longer";
            case GOOD -> "Good password";
            case STRONG -> "Strong password";
        };
    }
}
