package com.railreserve.ui.dashboard;

import com.railreserve.ui.AppFrame;
import com.railreserve.ui.UserSession;
import com.railreserve.ui.components.Toast;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen 4 — Dashboard shell: left sidebar (Overview, Book Ticket, My
 * Bookings, PNR Status, Cancel Ticket, Settings, Logout) and a right content
 * area rebuilt fresh for every section so data is always current. Requires a
 * session; the App router enforces that before showing this screen.
 */
public class DashboardScreen extends JPanel implements DashboardHost {

    /** Section definitions with lazily-built panels. */
    private record SectionDef(String label, java.util.function.Supplier<JPanel> factory) {
    }

    private final Map<String, SectionDef> sections = new LinkedHashMap<>();
    private final List<SidebarButton> sidebarButtons = new ArrayList<>();
    private com.railreserve.ui.SearchIntent pendingPrefill;
    private final JPanel content = new JPanel(new java.awt.CardLayout());
    private final Runnable onLogout;

    public DashboardScreen(Runnable onLogout) {
        this.onLogout = onLogout;
        buildSections();
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        add(sidebar(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        showSection("overview");
    }

    private void buildSections() {
        sections.put("overview", new SectionDef("Overview",
                () -> new OverviewPanel(this, UserSession.currentUser().getId())));
        sections.put("book", new SectionDef("Book Ticket",
                () -> new BookPanel(this, UserSession.currentUser().getId(), pendingPrefill)));
        sections.put("bookings", new SectionDef("My Bookings",
                () -> new MyBookingsPanel(this, UserSession.currentUser().getId())));
        sections.put("pnr", new SectionDef("PNR Status",
                () -> new PnrPanel(this)));
        sections.put("cancel", new SectionDef("Cancel Ticket",
                () -> new CancelPanel(this)));
    }

    /** White ixigo-style sidebar: brand on top, quiet links, account at the bottom. */
    private JComponent sidebar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setPreferredSize(new Dimension(248, 0));
        bar.setBackground(Theme.SURFACE);
        bar.setOpaque(true);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP12, Theme.SP24, Theme.SP12)));

        JPanel brandRow = new JPanel(new BorderLayout(Theme.SP8, 0));
        brandRow.setOpaque(false);
        brandRow.setAlignmentX(0f);
        brandRow.setBorder(BorderFactory.createEmptyBorder(0, Theme.SP8, 0, Theme.SP8));
        brandRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        brandRow.add(new JLabel(com.railreserve.ui.components.Icons.train(24)), BorderLayout.WEST);
        JLabel brand = new JLabel("RailReserve");
        brand.setFont(Theme.H2.deriveFont(18f));
        brand.setForeground(Theme.TEXT);
        brandRow.add(brand, BorderLayout.CENTER);
        bar.add(brandRow);

        JLabel tag = new JLabel("TRAIN BOOKING");
        tag.setFont(Theme.SMALL.deriveFont(Font.BOLD, 9.5f));
        tag.setForeground(Theme.MUTED);
        tag.setAlignmentX(0f);
        tag.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        tag.setBorder(BorderFactory.createEmptyBorder(Theme.SP2, Theme.SP16, 0, 0));
        bar.add(tag);
        bar.add(Box.createVerticalStrut(Theme.SP20));

        for (Map.Entry<String, SectionDef> e : sections.entrySet()) {
            SidebarButton button = new SidebarButton(e.getValue().label(), e.getKey());
            button.attach(a -> showSection(e.getKey()));
            sidebarButtons.add(button);
            bar.add(button);
            bar.add(Box.createVerticalStrut(Theme.SP2));
        }
        bar.add(Box.createVerticalGlue());

        bar.add(sessionChip());
        bar.add(Box.createVerticalStrut(Theme.SP8));

        SidebarButton theme = new SidebarButton(Theme.isDark() ? "Light mode" : "Dark mode", "theme");
        theme.attach(a -> toggleTheme());
        theme.setIcon(Theme.isDark()
                ? com.railreserve.ui.components.Icons.sun(15)
                : com.railreserve.ui.components.Icons.moon(15));
        bar.add(theme);
        bar.add(Box.createVerticalStrut(Theme.SP2));

        SidebarButton settings = new SidebarButton("Settings", "settings");
        settings.attach(a -> openSettings());
        bar.add(settings);
        bar.add(Box.createVerticalStrut(Theme.SP2));

        SidebarButton logout = new SidebarButton("Logout", "logout");
        logout.attach(a -> onLogout.run());
        bar.add(logout);
        return bar;
    }

    /** Small account block above Settings: avatar initial + display name. */
    private JComponent sessionChip() {
        JPanel chip = new JPanel(new BorderLayout(Theme.SP8, 0));
        chip.setOpaque(false);
        chip.setAlignmentX(0f);
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        chip.setBorder(BorderFactory.createEmptyBorder(0, Theme.SP8, Theme.SP8, Theme.SP8));

        String name = UserSession.displayName() == null ? "Guest" : UserSession.displayName();
        JLabel avatar = new JLabel(name.isBlank() ? "?" : name.substring(0, 1).toUpperCase(),
                SwingConstants.CENTER);
        avatar.setFont(Theme.BODY.deriveFont(Font.BOLD, 13f));
        avatar.setForeground(Theme.BRAND);
        avatar.setOpaque(true);
        avatar.setBackground(Theme.BRAND_SOFT);
        avatar.setPreferredSize(new Dimension(32, 32));
        chip.add(avatar, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel display = new JLabel(name);
        display.setFont(Theme.SMALL.deriveFont(Font.BOLD, 12.5f));
        display.setForeground(Theme.TEXT);
        JLabel handle = new JLabel("Logged in");
        handle.setFont(Theme.SMALL.deriveFont(Font.PLAIN, 10.5f));
        handle.setForeground(Theme.MUTED);
        text.add(display);
        text.add(handle);
        chip.add(text, BorderLayout.CENTER);
        return chip;
    }

    /** Opens Book Ticket with a landing-page search replayed into the form. */
    public void openBook(com.railreserve.ui.SearchIntent prefill) {
        this.pendingPrefill = prefill;
        showSection("book");
    }

    // ---------- DashboardHost ----------

    @Override
    public void showSection(String section) {
        SectionDef def = sections.get(section);
        if (def == null) {
            return;
        }
        content.removeAll();
        JPanel panel = def.factory().get();
        panel.setBackground(Theme.BACKGROUND);
        content.add(panel, section);
        markActive(section);
        ((java.awt.CardLayout) content.getLayout()).show(content, section);
        revalidate();
        repaint();
    }

    @Override
    public void toast(String message, Toast.Kind kind) {
        Toast.show(this, message, kind);
    }

    @Override
    public void openSettings() {
        AppFrame frame = AppFrame.instance();
        if (frame != null) {
            frame.show(AppFrame.SETTINGS);
        }
    }

    /** Quick theme switch from the sidebar: persists, toasts, rebuilds the screen. */
    private void toggleTheme() {
        boolean next = !Theme.isDark();
        AppFrame frame = AppFrame.instance();
        try {
            com.railreserve.service.PreferencesService.Prefs current =
                    new com.railreserve.service.PreferencesService().load();
            new com.railreserve.service.PreferencesService().save(
                    new com.railreserve.service.PreferencesService.Prefs(next,
                            current.defaultClass(), current.defaultSource()));
        } catch (java.io.IOException ex) {
            // theme still applies live; saving is best-effort
        }
        if (frame != null) {
            Toast.show(frame.layeredPane(), next ? "Dark mode on." : "Light mode on.",
                    Toast.Kind.INFO);
        }
        Theme.setDark(next);   // the shell rebuilds this screen with fresh colors
    }

    private void markActive(String section) {
        for (SidebarButton b : sidebarButtons) {
            b.setActive(section.equals(b.sectionKey));
        }
    }

    /** Sidebar link: soft-orange pill when active, light grey on hover, ink otherwise. */
    private static final class SidebarButton extends JPanel {

        private final String sectionKey;
        private final JLabel label = new JLabel();
        private boolean active;
        private boolean hover;

        SidebarButton(String text, String sectionKey) {
            this.sectionKey = sectionKey;
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(Theme.SP8, Theme.SP12, Theme.SP8, Theme.SP12));
            setPreferredSize(new Dimension(0, 38));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            add(label, BorderLayout.CENTER);
            setText(text);
            setActive(false);
            setCursor(Theme.HAND);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void attach(ActionListener listener) {
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    listener.actionPerformed(null);
                }
            });
        }

        void setText(String text) {
            label.setText(text);
        }

        void setIcon(javax.swing.Icon icon) {
            label.setIcon(icon);
            label.setIconTextGap(Theme.SP8);
        }

        void setActive(boolean active) {
            this.active = active;
            label.setFont(Theme.BODY.deriveFont(active ? Font.BOLD : Font.PLAIN, 13.5f));
            label.setForeground(active ? Theme.BRAND : Theme.TEXT);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(Theme.BRAND_SOFT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_SM, Theme.RADIUS_SM);
            } else if (hover) {
                g2.setColor(Theme.SURFACE_ALT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_SM, Theme.RADIUS_SM);
            }
            g2.dispose();
        }

        @Override
        public Insets getInsets() {
            return new Insets(Theme.SP8, Theme.SP12, Theme.SP8, Theme.SP12);
        }
    }
}
