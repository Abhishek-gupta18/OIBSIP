package com.railreserve.dao;

import com.railreserve.model.User;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Account storage used by AuthService and AccountService. Implemented by
 * {@link UserDAO}; tests substitute in-memory fakes so account rules are
 * verified without a DB.
 */
public interface AuthServiceStore {

    /** Inserts a new account; duplicate username or e-mail throws SQLState 23505. */
    User create(User user, String passwordHash) throws SQLException;

    /** True when a user with this username exists. */
    boolean exists(String username) throws SQLException;

    /** True when a user with this e-mail exists. */
    boolean existsEmail(String email) throws SQLException;

    /** Credential row by username OR e-mail (case-insensitive e-mail). */
    Optional<CredentialRecord> findCredential(String usernameOrEmail) throws SQLException;

    /**
     * Credential row by user id (Settings). Defaults to unsupported so
     * auth-only fakes stay small; UserDAO overrides it.
     */
    default Optional<CredentialRecord> findCredentialById(int userId) throws SQLException {
        throw new UnsupportedOperationException("findCredentialById");
    }

    /** Updates full name, e-mail and phone; duplicate e-mail throws 23505. */
    default void updateProfile(int userId, String fullName, String email, String phone) throws SQLException {
        throw new UnsupportedOperationException("updateProfile");
    }

    /** Stores a new PBKDF2 hash for the account. */
    default void updatePassword(int userId, String passwordHash) throws SQLException {
        throw new UnsupportedOperationException("updatePassword");
    }

    /**
     * Deletes the account and its reservations in one transaction. Documented
     * demo behaviour: a deleted account must not leave orphan bookings, and
     * the FK reservation.user_id makes user-first deletion impossible.
     */
    default void deleteAccount(int userId) throws SQLException {
        throw new UnsupportedOperationException("deleteAccount");
    }

    /** Credential row (id, username, password hash, full name). */
    record CredentialRecord(int id, String username, String passwordHash, String fullName) {
    }
}
