package com.railreserve.service;

import com.railreserve.dao.AuthServiceStore;
import com.railreserve.model.User;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sign-up flow against an in-memory AuthServiceStore fake (no database). */
class AuthServiceSignupTest {

    private static final class InMemoryStore implements AuthServiceStore {

        final Map<String, String> users = new HashMap<>();   // username -> hash
        boolean failNextSql = false;

        @Override
        public User create(User user, String passwordHash) throws SQLException {
            if (failNextSql) {
                throw new SQLException("boom", "08000", 0);
            }
            if (users.containsKey(user.getUsername())) {
                throw new SQLException("dup", "23505", 0);
            }
            if (passwordHash != null && users.values().contains(passwordHash) && user.getEmail() != null
                    && user.getEmail().contains("dup")) {
                throw new SQLException("email-dup", "23505", 0);
            }
            users.put(user.getUsername(), passwordHash);
            return user;
        }

        @Override
        public boolean exists(String username) {
            return users.containsKey(username);
        }

        @Override
        public boolean existsEmail(String email) {
            throw new UnsupportedOperationException(); // not used by the happy path
        }

        @Override
        public Optional<CredentialRecord> findCredential(String username) {
            return Optional.empty();
        }
    }

    private static AuthService.SignupRequest request(String username) {
        return new AuthService.SignupRequest(
                "Abhishek Gupta", username, username + "@example.com",
                "", "Sup3rSafe", "Sup3rSafe");
    }

    @Test
    void createsAccountAndHashesPassword() {
        InMemoryStore store = new InMemoryStore();
        AuthService service = new AuthService(store);

        AuthService.SignupOutcome outcome = service.signup(request("abhi"));

        assertTrue(outcome.success());
        assertNotNull(outcome.user());
        assertEquals("abhi", outcome.user().getUsername());
        assertEquals("abhi@example.com", outcome.user().getEmail());
        assertTrue(store.users.get("abhi").startsWith("pbkdf2$"),
                "stored value must be a PBKDF2 hash, not the raw password");
        assertTrue(outcome.message().startsWith("Account created"));
    }

    @Test
    void validationErrorsRefuseCreation() {
        InMemoryStore store = new InMemoryStore();
        AuthService service = new AuthService(store);

        AuthService.SignupOutcome outcome = service.signup(new AuthService.SignupRequest(
                "", "x", "bad", "abc", "weak", "weak2"));

        assertFalse(outcome.success());
        assertNull(outcome.user());
        assertNotNull(outcome.fieldErrors());
        assertTrue(outcome.fieldErrors().hasAny());
        assertTrue(store.users.isEmpty(), "invalid signup must not touch the store");
    }

    @Test
    void duplicateUsernameYieldsFriendlyInlineError() {
        InMemoryStore store = new InMemoryStore();
        AuthService service = new AuthService(store);
        store.users.put("abhi", "pbkdf2$120000$AAAA$AAAA==");

        AuthService.SignupOutcome outcome = service.signup(request("abhi"));

        assertFalse(outcome.success());
        assertNotNull(outcome.fieldErrors());
        assertEquals("This username is already taken — try another.", outcome.fieldErrors().username());
        assertNull(outcome.fieldErrors().email());
        assertEquals("That username is already taken.", outcome.message());
    }

    @Test
    void genericSqlFailureGetsFriendlyMessageNotStackTrace() {
        InMemoryStore store = new InMemoryStore();
        store.failNextSql = true;
        AuthService service = new AuthService(store);

        AuthService.SignupOutcome outcome = service.signup(request("newbie"));

        assertFalse(outcome.success());
        assertEquals("Could not create the account right now. Please try again shortly.",
                outcome.message());
    }

    @Test
    void emailDuplicateMapsToFriendlyMessage() {
        // store whose email-existence simulates the DB unique index on email
        AuthService store = new AuthService(new AuthServiceStore() {
            @Override
            public User create(User user, String passwordHash) throws SQLException {
                throw new SQLException("dup email", "23505", 0);
            }

            @Override
            public boolean exists(String username) {
                return false; // username free ⇒ duplicate must be the e-mail
            }

            @Override
            public boolean existsEmail(String email) {
                return true;
            }

            @Override
            public Optional<CredentialRecord> findCredential(String username) {
                return Optional.empty();
            }
        });

        AuthService.SignupOutcome outcome = store.signup(request("fresh"));

        assertFalse(outcome.success());
        assertNotNull(outcome.fieldErrors());
        assertEquals("This e-mail is already registered.", outcome.fieldErrors().email());
        assertEquals("An account with this e-mail already exists.", outcome.message());
    }
}
