package com.railreserve.service;

import com.railreserve.dao.AuthServiceStore;
import com.railreserve.model.User;
import com.railreserve.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Settings rules against an in-memory store; no database required. */
class AccountServiceTest {

    /** Fake account storage with one user id=9, password Sup3rSafe. */
    private static final class FakeStore implements AuthServiceStore {

        String hash = PasswordUtil.hash("Sup3rSafe");
        String fullName = "Old Name";
        String email = "old@example.com";
        String phone = null;
        boolean emailTaken;
        boolean deleted;
        String lastSavedHash;

        @Override
        public User create(User user, String passwordHash) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean exists(String username) {
            return false;
        }

        @Override
        public boolean existsEmail(String email) {
            return emailTaken;
        }

        @Override
        public Optional<CredentialRecord> findCredential(String usernameOrEmail) {
            return findCredentialById(9);
        }

        @Override
        public Optional<CredentialRecord> findCredentialById(int userId) {
            return userId == 9
                    ? Optional.of(new CredentialRecord(9, "abhi", hash, fullName))
                    : Optional.empty();
        }

        @Override
        public void updateProfile(int userId, String fullName, String email, String phone) throws SQLException {
            if (emailTaken) {
                throw new SQLException("dup email", "23505", 0);
            }
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
        }

        @Override
        public void updatePassword(int userId, String passwordHash) {
            this.hash = passwordHash;
            this.lastSavedHash = passwordHash;
        }

        @Override
        public void deleteAccount(int userId) {
            this.deleted = true;
        }
    }

    @Test
    void profileValidationRejectsBadEmail() {
        FakeStore store = new FakeStore();
        AccountService service = new AccountService(store);

        AccountService.ProfileOutcome outcome = service.updateProfile(9, "New Name", "not-an-email", "");

        assertFalse(outcome.success());
        assertNotNull(outcome.errors().email());
        assertEquals("Old Name", store.fullName, "invalid profile must not be saved");
    }

    @Test
    void profileUpdateSavesStrippedValues() {
        FakeStore store = new FakeStore();
        AccountService service = new AccountService(store);

        AccountService.ProfileOutcome outcome = service.updateProfile(9, "  New Name ", " new@example.com ", "9876543210");

        assertTrue(outcome.success());
        assertEquals("New Name", store.fullName);
        assertEquals("new@example.com", store.email);
        assertEquals("9876543210", store.phone);
    }

    @Test
    void duplicateEmailMapsToInlineFieldError() {
        FakeStore store = new FakeStore();
        store.emailTaken = true;
        AccountService service = new AccountService(store);

        AccountService.ProfileOutcome outcome = service.updateProfile(9, "Name", "taken@example.com", "");

        assertFalse(outcome.success());
        assertEquals("This e-mail is already used by another account.", outcome.errors().email());
        assertEquals("That e-mail is already registered.", outcome.message());
    }

    @Test
    void changePasswordRequiresCorrectCurrentPassword() {
        FakeStore store = new FakeStore();
        AccountService service = new AccountService(store);

        AccountService.PasswordOutcome outcome = service.changePassword(9, "wrong", "NewPass1", "NewPass1");

        assertFalse(outcome.success());
        assertEquals("Current password is incorrect.", outcome.message());
        assertNull(store.lastSavedHash);
    }

    @Test
    void changePasswordEnforcesPolicyAndMatch() {
        FakeStore store = new FakeStore();
        AccountService service = new AccountService(store);

        AccountService.PasswordOutcome weak = service.changePassword(9, "Sup3rSafe", "weak", "weak");
        assertFalse(weak.success());
        assertNotNull(weak.errors().password());

        AccountService.PasswordOutcome mismatch =
                service.changePassword(9, "Sup3rSafe", "NewPass1", "NewPass2");
        assertFalse(mismatch.success());
        assertNotNull(mismatch.errors().confirm());
    }

    @Test
    void changePasswordStoresNewHash() {
        FakeStore store = new FakeStore();
        AccountService service = new AccountService(store);

        AccountService.PasswordOutcome outcome = service.changePassword(9, "Sup3rSafe", "NewPass1", "NewPass1");

        assertTrue(outcome.success());
        assertNotNull(store.lastSavedHash);
        assertTrue(store.lastSavedHash.startsWith("pbkdf2$"));
        assertTrue(PasswordUtil.verify("NewPass1", store.lastSavedHash));
    }

    @Test
    void deleteRequiresTypedWordAndPassword() {
        FakeStore store = new FakeStore();
        AccountService service = new AccountService(store);

        assertFalse(service.deleteAccount(9, "Sup3rSafe", "delete").success(), "lowercase not accepted");
        assertFalse(service.deleteAccount(9, "", "DELETE").success(), "password required");
        assertFalse(service.deleteAccount(9, "wrong", "DELETE").success(), "wrong password refused");
        assertFalse(store.deleted);
    }

    @Test
    void deleteRemovesAccountWhenConfirmed() {
        FakeStore store = new FakeStore();
        AccountService service = new AccountService(store);

        AccountService.DeleteOutcome outcome = service.deleteAccount(9, "Sup3rSafe", "DELETE");

        assertTrue(outcome.success());
        assertTrue(store.deleted);
        assertTrue(outcome.message().contains("deleted"));
    }
}
