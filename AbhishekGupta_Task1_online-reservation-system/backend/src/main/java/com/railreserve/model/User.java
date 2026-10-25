package com.railreserve.model;

/** Application user. Only the PBKDF2 hash is ever carried around. */
public class User {

    private final int id;
    private final String username;
    private final String fullName;
    private final String email;
    private final String phone;

    public User(int id, String username) {
        this(id, username, null, null, null);
    }

    /** Full profile, used by sign-up creation. */
    public User(String username, String fullName, String email, String phone) {
        this(0, username, fullName, email, phone);
    }

    public User(int id, String username, String fullName, String email, String phone) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return username;
    }
}
