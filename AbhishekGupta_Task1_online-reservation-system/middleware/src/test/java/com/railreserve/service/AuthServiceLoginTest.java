package com.railreserve.service;

import com.railreserve.dao.AuthServiceStore;
import com.railreserve.model.User;
import com.railreserve.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Login rules against an in-memory AuthServiceStore (no database). */
class AuthServiceLoginTest {

    /** Store with one confirmed account: abhi / Sup3rSafe (hash computed here). */
    private static final class SingleAccountStore implements AuthServiceStore {

        final Map<String, String> usernames = new HashMap<>();       // username -> hash
        final Map<String, String> emails = new HashMap<>();          // lower(email) -> username

        SingleAccountStore() {
            usernames.put("abhi", PasswordUtil.hash("Sup3rSafe"));
            emails.put("abhi@example.com", "abhi");
        }

        @Override
        public User create(User user, String passwordHash) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean exists(String username) {
            return usernames.containsKey(username);
        }

        @Override
        public boolean existsEmail(String email) {
            return emails.containsKey(email.toLowerCase());
        }

        @Override
        public Optional<CredentialRecord> findCredential(String usernameOrEmail) {
            String username = usernames.containsKey(usernameOrEmail)
                    ? usernameOrEmail
                    : emails.get(usernameOrEmail.toLowerCase());
            if (username == null) {
                return Optional.empty();
            }
            return Optional.of(new CredentialRecord(7, username, usernames.get(username), "Abhishek Gupta"));
        }
    }

    private final AuthService service = new AuthService(new SingleAccountStore());

    @Test
    void loginByUsernameReturnsUserWithFullName() {
        AuthService.LoginResult result = service.login("abhi", "Sup3rSafe");
        assertTrue(result.success());
        assertEquals("abhi", result.user().getUsername());
        assertEquals("Abhishek Gupta", result.user().getFullName());
        assertNull(result.errorMessage());
    }

    @Test
    void loginByEmailIsCaseInsensitive() {
        assertTrue(service.login("ABHI@EXAMPLE.com", "Sup3rSafe").success());
        assertTrue(service.login("abhi@example.com", "Sup3rSafe").success());
    }

    @Test
    void wrongPasswordAndUnknownUserShareOneMessage() {
        AuthService.LoginResult wrongPassword = service.login("abhi", "nope");
        AuthService.LoginResult unknownUser = service.login("ghost", "Sup3rSafe");
        assertEquals("Invalid username or password.", wrongPassword.errorMessage());
        assertEquals("Invalid username or password.", unknownUser.errorMessage());
        assertNull(unknownUser.user());
    }

    @Test
    void blankInputsGetSpecificMessages() {
        assertEquals("Please enter a username.", service.login("", "Sup3rSafe").errorMessage());
        assertEquals("Please enter a password.", service.login("abhi", "").errorMessage());
    }
}
