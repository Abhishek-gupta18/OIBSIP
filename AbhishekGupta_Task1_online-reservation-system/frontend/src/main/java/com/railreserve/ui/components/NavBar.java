package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Landing-page top bar (ixigo style): orange train mark + bold wordmark on the
 * left, quiet grey links in the middle, Login/Sign Up (or the account dropdown)
 * on the right. Sits on white with a single hairline underneath.
 */
public class NavBar extends JPanel {

    private final Map<String, JButton> links = new LinkedHashMap<>();
    private final JPanel rightActions;
    private String active;

    public NavBar(List<String> linkLabels, Consumer<String> onLink) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(Theme.SP12, Theme.SP32, Theme.SP12, Theme.SP32)));

        JLabel logo = new JLabel("RailReserve", Icons.train(24), JLabel.LEFT);
        logo.setIconTextGap(Theme.SP8);
        logo.setFont(Theme.H2.deriveFont(19f));
        logo.setForeground(Theme.TEXT);
        logo.setCursor(Theme.HAND);
        logo.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                onLink.accept("Home");
            }
        });
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.add(logo);
        add(brand, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, Theme.SP20, 0));
        center.setOpaque(false);
        for (String name : linkLabels) {
            JButton link = new JButton(name);
            link.setContentAreaFilled(false);
            link.setBorderPainted(false);
            link.setFocusPainted(false);
            link.setOpaque(false);
            link.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            link.setFont(Theme.BODY.deriveFont(Font.PLAIN, 13.5f));
            link.setForeground(Theme.MUTED);
            link.setCursor(Theme.HAND);
            link.addActionListener(e -> onLink.accept(name));
            link.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (!name.equals(active)) {
                        link.setForeground(Theme.BRAND);
                    }
                }

                @Override public void mouseExited(MouseEvent e) {
                    if (!name.equals(active)) {
                        link.setForeground(Theme.MUTED);
                    }
                }
            });
            links.put(name, link);
            center.add(link);
        }
        add(center, BorderLayout.CENTER);

        rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SP8, 0));
        rightActions.setOpaque(false);
        add(rightActions, BorderLayout.EAST);
    }

    /** Adds the pre-login actions on the right. */
    public void addLoginActions(java.awt.event.ActionListener onLogin,
                                java.awt.event.ActionListener onSignUp) {
        PrimaryButton signUp = new PrimaryButton("Sign up");
        signUp.setFont(Theme.BODY.deriveFont(Font.BOLD, 13f));
        SecondaryButton login = new SecondaryButton("Login");
        login.addActionListener(onLogin);
        signUp.addActionListener(onSignUp);
        rightActions.removeAll();
        rightActions.add(login);
        rightActions.add(signUp);
        revalidate();
        repaint();
    }

    /**
     * Post-login right side: avatar + name that opens a dropdown with
     * Dashboard, Settings and Logout. Each selection is reported back by name
     * so the router decides what to do.
     */
    public void addSessionActions(String displayName, Consumer<String> onSessionAction) {
        rightActions.removeAll();
        SecondaryButton account = new SecondaryButton(displayName);
        account.setIcon(Icons.user(14));
        account.setIconTextGap(Theme.SP8);
        account.setToolTipText("Account menu");

        JPopupMenu menu = new JPopupMenu();
        for (String item : List.of("Dashboard", "Settings", "Logout")) {
            JMenuItem menuItem = new JMenuItem(item);
            menuItem.setFont(Theme.BODY);
            menuItem.addActionListener(e -> onSessionAction.accept(item));
            menu.add(menuItem);
        }

        account.addActionListener(e -> menu.show(account, 0, account.getHeight()));
        rightActions.add(account);
        revalidate();
        repaint();
    }

    /** Highlights the active link in brand orange. */
    public void setActive(String name) {
        active = name;
        links.forEach((label, button) -> {
            boolean on = label.equals(name);
            button.setForeground(on ? Theme.BRAND : Theme.MUTED);
            button.setFont(Theme.BODY.deriveFont(on ? Font.BOLD : Font.PLAIN, 13.5f));
            button.setCursor(on ? Cursor.getDefaultCursor() : Theme.HAND);
        });
    }

}
