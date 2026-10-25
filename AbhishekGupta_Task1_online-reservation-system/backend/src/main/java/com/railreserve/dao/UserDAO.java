package com.railreserve.dao;

import com.railreserve.config.DatabaseConfig;
import com.railreserve.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * JDBC data access for the users table. All SQL uses PreparedStatement.
 * Duplicate username/email surface as SQLState 23505 from insert(); the
 * service maps that to friendly messages (see AuthService.signup).
 */
public class UserDAO implements AuthServiceStore {

    /**
     * Finds a user by username OR e-mail (case-insensitive), returning the
     * id, username, full name and stored password hash for AuthService.
     */
    @Override
    public Optional<AuthServiceStore.CredentialRecord> findCredential(String usernameOrEmail) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name FROM users "
                + "WHERE username = ? OR (email IS NOT NULL AND lower(email) = lower(?))";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new AuthServiceStore.CredentialRecord(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name")));
                }
                return Optional.empty();
            }
        }
    }

    /** Returns true when the username already exists. */
    public boolean exists(String username) throws SQLException {
        return findCredential(username).isPresent();
    }

    /** Returns true when the e-mail is already registered (NULLs never match). */
    public boolean existsEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE lower(email) = lower(?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Inserts a new account (password pre-hashed by the caller). Throws
     * SQLException with SQLState 23505 on duplicate username or e-mail.
     */
    @Override
    public User create(User user, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, phone) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConfig.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, passwordHash);
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new User(keys.getInt(1), user.getUsername(),
                            user.getFullName(), user.getEmail(), user.getPhone());
                }
            }
            return user;
        }
    }

    // ---------- Settings: profile, password, deletion ----------

    @Override
    public Optional<AuthServiceStore.CredentialRecord> findCredentialById(int userId) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name FROM users WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new AuthServiceStore.CredentialRecord(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name")));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public void updateProfile(int userId, String fullName, String email, String phone) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void updatePassword(int userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** Transaction: remove the user's reservations, then the account itself. */
    @Override
    public void deleteAccount(int userId) throws SQLException {
        java.sql.Connection conn = DatabaseConfig.getConnection();
        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (PreparedStatement deleteReservations = conn.prepareStatement(
                "DELETE FROM reservations WHERE user_id = ?");
             PreparedStatement deleteUser = conn.prepareStatement(
                     "DELETE FROM users WHERE id = ?")) {
            deleteReservations.setInt(1, userId);
            deleteReservations.executeUpdate();
            deleteUser.setInt(1, userId);
            deleteUser.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            try {
                conn.setAutoCommit(previousAutoCommit);
            } finally {
                conn.close();
            }
        }
    }
}
