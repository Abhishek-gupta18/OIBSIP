package com.railreserve.service;

import com.railreserve.dao.AuthServiceStore;
import com.railreserve.dao.UserDAO;
import com.railreserve.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Settings business logic: profile update, password change (current password
 * must verify first) and account deletion (typed confirmation + password).
 * Passwords are compared/hashed as PBKDF2 and never logged. Testable through
 * the {@link AuthServiceStore} seam without a database.
 */
public class AccountService {

    public record ProfileOutcome(boolean success, SignupValidator.FieldErrors errors, String message) {
        static ProfileOutcome ok(String message) {
            return new ProfileOutcome(true, null, message);
        }
    }

    public record PasswordOutcome(boolean success, SignupValidator.FieldErrors errors, String message) {
        static PasswordOutcome ok() {
            return new PasswordOutcome(true, null, "Password updated.");
        }

        static PasswordOutcome fail(SignupValidator.FieldErrors errors, String message) {
            return new PasswordOutcome(false, errors, message);
        }
    }

    public record DeleteOutcome(boolean success, String message) {
    }

    private final AuthServiceStore store;

    public AccountService() {
        this(new UserDAO());
    }

    /** Test seam. */
    public AccountService(AuthServiceStore store) {
        this.store = store;
    }

    // ---------- profile ----------

    public ProfileOutcome updateProfile(int userId, String fullName, String email, String phone) {
        SignupValidator.FieldErrors errors = new SignupValidator.FieldErrors(
                SignupValidator.fullNameError(fullName),
                null,
                SignupValidator.emailError(email),
                SignupValidator.phoneError(phone),
                null,
                null);
        if (errors.hasAny()) {
            return new ProfileOutcome(false, errors, "Please fix the highlighted fields.");
        }
        try {
            store.updateProfile(userId, fullName.strip(), email.strip(),
                    phone == null || phone.isBlank() ? null : phone.strip());
            return ProfileOutcome.ok("Profile updated.");
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                return new ProfileOutcome(false, new SignupValidator.FieldErrors(
                        null, null, "This e-mail is already used by another account.", null, null, null),
                        "That e-mail is already registered.");
            }
            return new ProfileOutcome(false, null,
                    "Could not save your profile right now. Please try again shortly.");
        }
    }

    // ---------- password ----------

    public PasswordOutcome changePassword(int userId, String currentPassword,
                                          String newPassword, String confirm) {
        if (currentPassword == null || currentPassword.isEmpty()) {
            return PasswordOutcome.fail(new SignupValidator.FieldErrors(
                    null, null, null, null, null, null), "Enter your current password.");
        }
        String policyError = SignupValidator.passwordError(newPassword);
        String mismatch = SignupValidator.mismatchError(newPassword, confirm);
        if (policyError != null || mismatch != null) {
            return PasswordOutcome.fail(new SignupValidator.FieldErrors(
                    null, null, null, null, policyError, mismatch),
                    "Please fix the highlighted password fields.");
        }
        if (newPassword.equals(currentPassword)) {
            return PasswordOutcome.fail(new SignupValidator.FieldErrors(
                    null, null, null, null, "Choose a password different from the current one.", null),
                    "The new password must differ from the current one.");
        }
        try {
            Optional<AuthServiceStore.CredentialRecord> record = store.findCredentialById(userId);
            if (record.isEmpty()) {
                return PasswordOutcome.fail(null, "Your account could not be found — please sign in again.");
            }
            if (!PasswordUtil.verify(currentPassword, record.get().passwordHash())) {
                return PasswordOutcome.fail(new SignupValidator.FieldErrors(
                        null, null, null, null, null, null),
                        "Current password is incorrect.");
            }
            store.updatePassword(userId, PasswordUtil.hash(newPassword));
            return PasswordOutcome.ok();
        } catch (SQLException e) {
            return PasswordOutcome.fail(null,
                    "Could not change the password right now. Please try again shortly.");
        }
    }

    // ---------- delete account ----------

    /**
     * Deletes the account after password + typed-word confirmation. The DAO
     * removes the user's reservations first (same transaction) so no orphan
     * rows remain — documented demo behaviour on the Settings screen.
     */
    public DeleteOutcome deleteAccount(int userId, String password, String typedConfirmation) {
        if (typedConfirmation == null || !"DELETE".equals(typedConfirmation.strip())) {
            return new DeleteOutcome(false, "Type DELETE in capitals to confirm.");
        }
        if (password == null || password.isEmpty()) {
            return new DeleteOutcome(false, "Enter your password to delete the account.");
        }
        try {
            Optional<AuthServiceStore.CredentialRecord> record = store.findCredentialById(userId);
            if (record.isEmpty()) {
                return new DeleteOutcome(false, "Your account could not be found — please sign in again.");
            }
            if (!PasswordUtil.verify(password, record.get().passwordHash())) {
                return new DeleteOutcome(false, "Password is incorrect.");
            }
            store.deleteAccount(userId);
            return new DeleteOutcome(true, "Your account and its bookings were deleted.");
        } catch (SQLException e) {
            return new DeleteOutcome(false,
                    "Could not delete the account right now. Please try again shortly.");
        }
    }
}
