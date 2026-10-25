package com.railreserve.service;

import com.railreserve.dao.AuthServiceStore;
import com.railreserve.dao.UserDAO;
import com.railreserve.model.User;
import com.railreserve.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Auth business logic: login against the users table plus self-service
 * sign-up. Passwords are only ever compared/hashed as PBKDF2; raw passwords
 * are never logged.
 */
public class AuthService {

    /** Result of a login attempt. On failure, errorMessage is user-facing. */
    public record LoginResult(boolean success, User user, String errorMessage) {

        public static LoginResult ok(User user) {
            return new LoginResult(true, user, null);
        }

        public static LoginResult fail(String message) {
            return new LoginResult(false, null, message);
        }
    }

    /** Sign-up input bundle (password kept transient; never logged anywhere). */
    public record SignupRequest(String fullName, String username, String email,
                                String phone, String password, String confirm) {
    }

    /**
     * Sign-up outcome: per-field inline errors on refusal, a friendly banner
     * message, and the created user on success.
     */
    public record SignupOutcome(boolean success, SignupValidator.FieldErrors fieldErrors,
                                String message, User user) {

        public static SignupOutcome ok(User user, String message) {
            return new SignupOutcome(true, null, message, user);
        }

        public static SignupOutcome fail(SignupValidator.FieldErrors errors, String message) {
            return new SignupOutcome(false, errors, message, null);
        }
    }

    private final AuthServiceStore store;

    public AuthService(UserDAO userDAO) {
        this((AuthServiceStore) userDAO);
    }

    /** Test seam: a fake store replaces the database in unit tests. */
    public AuthService(AuthServiceStore store) {
        this.store = store;
    }

    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank()) {
            return LoginResult.fail("Please enter a username.");
        }
        if (password == null || password.isEmpty()) {
            return LoginResult.fail("Please enter a password.");
        }

        try {
            Optional<AuthServiceStore.CredentialRecord> record = store.findCredential(username.trim());
            if (record.isEmpty() || !PasswordUtil.verify(password, record.get().passwordHash())) {
                // Same message for unknown user and wrong password: do not leak which one failed
                return LoginResult.fail("Invalid username or password.");
            }
            AuthServiceStore.CredentialRecord found = record.get();
            User user = new User(found.id(), found.username(), found.fullName(), null, null);
            return LoginResult.ok(user);
        } catch (SQLException e) {
            return LoginResult.fail("Sign-in failed — the database is not reachable right now. Please try again shortly.");
        }
    }

    // ---------- sign-up ----------

    /** Validates and creates an account; duplicates get friendly inline errors. */
    public SignupOutcome signup(SignupRequest request) {
        SignupValidator.FieldErrors errors = SignupValidator.validate(
                request.fullName(), request.username(), request.email(),
                request.phone(), request.password(), request.confirm());
        if (errors.hasAny()) {
            return SignupOutcome.fail(errors, "Please fix the highlighted fields.");
        }

        User candidate = new User(
                request.username().strip(),
                request.fullName().strip(),
                request.email().strip(),
                request.phone() == null ? null : request.phone().strip());

        try {
            // Raw password only used here for hashing — never printed.
            User created = store.create(candidate, PasswordUtil.hash(request.password()));
            return SignupOutcome.ok(created, "Account created. You can sign in now.");
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                try {
                    if (store.exists(candidate.getUsername())) {
                        return SignupOutcome.fail(field("username", "This username is already taken — try another."),
                                "That username is already taken.");
                    }
                    return SignupOutcome.fail(field("email", "This e-mail is already registered."),
                            "An account with this e-mail already exists.");
                } catch (SQLException nested) {
                    return SignupOutcome.fail(null,
                            "Username or e-mail is already registered.");
                }
            }
            return SignupOutcome.fail(null,
                    "Could not create the account right now. Please try again shortly.");
        }
    }

    private static SignupValidator.FieldErrors field(String key, String message) {
        return new SignupValidator.FieldErrors(
                "fullName".equals(key) ? message : null,
                "username".equals(key) ? message : null,
                "email".equals(key) ? message : null,
                null, null, null);
    }
}
