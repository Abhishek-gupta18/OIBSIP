package com.railreserve;

import com.railreserve.config.DatabaseConfig;
import com.railreserve.ui.App;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point: configures look and feel, verifies the database,
 * then opens the one-window navigation shell on the landing screen.
 */
public final class Main {

    public static void main(String[] args) {
        configureLookAndFeel();

        if (!DatabaseConfig.isConnectionAvailable()) {
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Could not connect to the database:\n" + DatabaseConfig.describeTarget()
                            + "\n\nCheck DB_URL / DB_USER / DB_PASSWORD (or your .env file)\nand make sure PostgreSQL is reachable.",
                    "RailReserve — Database Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(App::launch);
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default Swing look and feel
        }
    }

    private Main() {
    }
}
