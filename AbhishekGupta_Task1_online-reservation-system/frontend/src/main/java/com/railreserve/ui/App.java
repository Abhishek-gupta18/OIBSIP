package com.railreserve.ui;

import com.railreserve.service.LoginAttemptTracker;
import com.railreserve.ui.components.Toast;
import com.railreserve.ui.dashboard.DashboardScreen;
import com.railreserve.ui.landing.LandingPanel;
import com.railreserve.ui.theme.Theme;

/**
 * Application controller: owns the single window, registers every screen,
 * routes navbar/link clicks and hero-search, keeps the pending search intent
 * and the login lockout state across navigations. UI-only — no SQL here.
 */
public class App {

    private final AppFrame frame = new AppFrame();
    private final LoginAttemptTracker loginTracker = new LoginAttemptTracker();
    private SearchIntent pendingSearch;

    /** Wires the shells together and opens the window on the landing page. */
    public static void launch() {
        // apply the saved theme before the first screen is built
        Theme.setDark(new com.railreserve.service.PreferencesService().load().darkMode());
        App app = new App();
        app.registerScreens();
        app.frame.show(AppFrame.HOME);
        app.frame.setVisible(true);
    }

    private void registerScreens() {
        frame.register(AppFrame.HOME, () ->
                new LandingPanel(this::handleNavLink, this::handleSearch,
                        () -> frame.show(AppFrame.LOGIN),
                        () -> frame.show(AppFrame.SIGNUP),
                        UserSession.isLoggedIn() ? UserSession.displayName() : null,
                        this::handleSessionAction));
        frame.register(AppFrame.LOGIN, () -> new LoginScreen(
                loginTracker,
                message -> {
                    resumePendingSearch();
                    Toast.show(frame.layeredPane(), message, Toast.Kind.SUCCESS);
                },
                () -> frame.show(AppFrame.SIGNUP)));
        frame.register(AppFrame.SIGNUP, () -> new SignUpScreen(
                message -> {
                    // success: land on Login with a confirmation toast
                    frame.show(AppFrame.LOGIN);
                    Toast.show(frame.layeredPane(), message, Toast.Kind.SUCCESS);
                },
                () -> frame.show(AppFrame.LOGIN)));
        frame.register(AppFrame.DASHBOARD, () -> new DashboardScreen(this::logout));
        frame.register(AppFrame.SETTINGS, () -> new SettingsScreen(
                () -> openDashboard("overview"),
                () -> {
                    UserSession.logout();
                    frame.show(AppFrame.HOME);
                    Toast.show(frame.layeredPane(),
                            "Your account was deleted.", Toast.Kind.SUCCESS);
                }));
        frame.register(AppFrame.ABOUT, () -> new TextPage("About RailReserve",
                "RailReserve is an academic OASIS Infobyte task: a Java 17 + Swing desktop "
                        + "application for train reservations backed by PostgreSQL.",
                "It demonstrates layered architecture (UI → Service → DAO), JDBC, unit testing "
                        + "and secure credential handling. It is not an official railway system."));
        frame.register(AppFrame.HELP, () -> new TextPage("Help",
                "Booking: log in, open Book Ticket, enter a train number, pick class and date, "
                        + "confirm — every confirmed booking gets a unique 10-digit PNR.",
                "Login trouble: five wrong attempts lock sign-in for 30 seconds; wait for the "
                        + "countdown, then try again.",
                "PNR status: enter your PNR on the landing page or under PNR Status after login.",
                "Cancellation: My Bookings → Cancel, or Cancel Ticket by PNR; cancellations are "
                        + "final in this demo.",
                "Cannot connect? Check the database settings in your .env file."));
        frame.register(AppFrame.PRIVACY, () -> new TextPage("Privacy",
                "This demo stores only what you type into it: name, username, e-mail, phone, and "
                        + "your bookings. Passwords are hashed — never stored in clear text.",
                "Nothing is shared with any external service, and no analytics run inside the app."));
    }

    /** Central router for every NavBar / footer link click. */
    private void handleNavLink(String target) {
        switch (target) {
            case "Home" -> frame.show(AppFrame.HOME);
            case "Book Tickets", "PNR Status", "Cancel Ticket" -> {
                if (UserSession.isLoggedIn()) {
                    String section = switch (target) {
                        case "Book Tickets" -> "book";
                        case "PNR Status" -> "pnr";
                        default -> "cancel";
                    };
                    openDashboard(section);
                } else {
                    Toast.show(frame.layeredPane(),
                            "Please login first — \"" + target + "\" opens after login.",
                            Toast.Kind.INFO);
                    frame.show(AppFrame.LOGIN);
                }
            }
            case "Help" -> frame.show(AppFrame.HELP);
            case "About" -> frame.show(AppFrame.ABOUT);
            case "Privacy" -> frame.show(AppFrame.PRIVACY);
            default -> frame.show(AppFrame.HOME);
        }
    }

    /** Hero-search: remember the search, then send the visitor to login. */
    private void handleSearch(SearchIntent intent) {
        pendingSearch = intent;
        if (UserSession.isLoggedIn()) {
            applyPendingSearch();
            return;
        }
        Toast.show(frame.layeredPane(),
                "Search saved — login to finish the booking.", Toast.Kind.INFO);
        frame.show(AppFrame.LOGIN);
    }

    /** After login: resume the saved search straight into the booking form. */
    private void resumePendingSearch() {
        if (pendingSearch != null) {
            applyPendingSearch();
        } else {
            frame.show(AppFrame.DASHBOARD);
        }
    }

    private void applyPendingSearch() {
        SearchIntent intent = pendingSearch;
        pendingSearch = null;
        openDashboard("book");
        com.railreserve.ui.dashboard.DashboardScreen dashboard =
                frame.activePanel() instanceof com.railreserve.ui.dashboard.DashboardScreen d ? d : null;
        if (dashboard != null && intent != null) {
            dashboard.openBook(intent);
        }
    }

    private void openDashboard(String section) {
        frame.show(AppFrame.DASHBOARD);
        if (frame.activePanel() instanceof com.railreserve.ui.dashboard.DashboardScreen dashboard) {
            dashboard.showSection(section);
        }
    }

    /** Navbar account dropdown: Dashboard, Settings, Logout. */
    private void handleSessionAction(String action) {
        switch (action) {
            case "Dashboard" -> openDashboard("overview");
            case "Settings" -> frame.show(AppFrame.SETTINGS);
            default -> logout();
        }
    }

    private void logout() {
        UserSession.logout();
        frame.show(AppFrame.HOME);
        Toast.show(frame.layeredPane(), "Logged out.", Toast.Kind.SUCCESS);
    }

    /** The intent captured before the login redirect, or null. */
    public SearchIntent pendingSearch() {
        return pendingSearch;
    }

    public void clearPendingSearch() {
        pendingSearch = null;
    }
}
