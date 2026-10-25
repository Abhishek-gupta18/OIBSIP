package com.railreserve.ui;

import com.railreserve.ui.theme.Theme;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The one-and-only application window. Screens live as named cards inside a
 * CardLayout; every show(key) rebuilds that screen fresh from its factory
 * (session state, filters, theme) before displaying it. A theme flip reruns
 * the active screen so all colors update with no per-widget listeners.
 */
public class AppFrame extends JFrame {

    public static final String HOME = "home";
    public static final String LOGIN = "login";
    public static final String SIGNUP = "signup";
    public static final String DASHBOARD = "dashboard";
    public static final String SETTINGS = "settings";
    public static final String ABOUT = "about";
    public static final String HELP = "help";
    public static final String PRIVACY = "privacy";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Supplier<JPanel>> screenFactories = new LinkedHashMap<>();
    private final Map<String, JPanel> currentCards = new LinkedHashMap<>();
    private String activeKey;
    /** Held in a field so the registration outlives garbage collection. */
    private final Runnable themeChangeListener = () -> {
        String keep = activeKey;
        if (keep != null) {
            show(keep);
        }
    };

    public AppFrame() {
        setTitle("RailReserve");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        pack();
        Dimension screen = GraphicsEnvironment.isHeadless()
                ? new Dimension(1280, 800)
                : Toolkit.getDefaultToolkit().getScreenSize();
        setSize(new Dimension(Math.min(1280, screen.width - 80), Math.min(800, screen.height - 80)));
        setLocationRelativeTo(null);
        add(content);

        Theme.onThemeChange(themeChangeListener);
    }

    public void register(String key, Supplier<JPanel> factory) {
        screenFactories.put(key, factory);
    }

    /** Builds the screen fresh, replaces any previous card of that key, shows it. */
    public void show(String key) {
        Supplier<JPanel> factory = screenFactories.get(key);
        if (factory == null) {
            return;
        }
        JPanel previous = currentCards.remove(key);
        if (previous != null) {
            content.remove(previous);
        }
        JPanel screen = factory.get();
        currentCards.put(key, screen);
        content.add(screen, key);
        activeKey = key;
        cards.show(content, key);
        revalidate();
        repaint();
    }

    public String activeScreen() {
        return activeKey;
    }

    /** Currently displayed card (e.g. to open a specific dashboard section). */
    public JPanel activePanel() {
        return activeKey == null ? null : currentCards.get(activeKey);
    }

    /** Toasts attach to the window's layered pane. */
    public JLayeredPane layeredPane() {
        return getLayeredPane();
    }

    /** The one live frame across windows, or null in headless/test setups. */
    public static AppFrame instance() {
        for (java.awt.Window w : java.awt.Window.getWindows()) {
            if (w instanceof AppFrame frame) {
                return frame;
            }
        }
        return null;
    }
}
