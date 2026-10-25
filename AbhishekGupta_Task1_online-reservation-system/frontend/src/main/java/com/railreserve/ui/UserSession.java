package com.railreserve.ui;

import com.railreserve.model.User;

/** Holds the logged-in user for the UI layer; the service layer stays stateless. */
public final class UserSession {

    private static User current;

    private UserSession() {
    }

    public static void login(User user) {
        current = user;
    }

    /** Clears the session (Logout). */
    public static void logout() {
        current = null;
    }

    public static User currentUser() {
        return current;
    }

    public static boolean isLoggedIn() {
        return current != null;
    }

    /** Greeting name: full name when known, otherwise the username. */
    public static String displayName() {
        if (current == null) {
            return "";
        }
        return (current.getFullName() == null || current.getFullName().isBlank())
                ? current.getUsername()
                : current.getFullName();
    }
}
