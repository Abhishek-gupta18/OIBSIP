package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

/** Dark, rounded snackbar at the bottom of the window; auto-dismisses. Never blocks. */
public final class Toast {

    public enum Kind { SUCCESS, ERROR, INFO }

    /** Ink pill in both themes — ixigo's snackbars stay dark on purpose. */
    private static final Color INK = new Color(0x1E222C);

    private Toast() {
    }

    public static void show(JLayeredPane layered, String message, Kind kind) {
        ToastPanel toast = new ToastPanel(message, kind);
        java.awt.Dimension pref = toast.getPreferredSize();
        int x = Math.max(Theme.SP16, (layered.getWidth() - pref.width) / 2);
        int y = layered.getHeight() - pref.height - Theme.SP24;
        toast.setBounds(x, Math.max(Theme.SP16, y), pref.width, pref.height);
        layered.setLayer(toast, JLayeredPane.POPUP_LAYER);
        layered.add(toast);
        new Timer(2600, e -> layered.remove(toast)).start();
    }

    public static void show(javax.swing.JComponent host, String message, Kind kind) {
        JLayeredPane layered = host.getRootPane() != null
                ? host.getRootPane().getLayeredPane() : null;
        if (layered != null) {
            show(layered, message, kind);
        }
    }

    private static final class ToastPanel extends JPanel {
        private final Color dot;

        private ToastPanel(String message, Kind kind) {
            super(new GridBagLayout());
            setOpaque(false);
            dot = switch (kind) {
                case SUCCESS -> Theme.SUCCESS;
                case ERROR -> Theme.ERROR;
                case INFO -> Theme.BRAND;
            };
            JLabel label = new JLabel(message, JLabel.CENTER);
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Segoe UI", Font.BOLD, 13));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(Theme.SP12, Theme.SP20, Theme.SP12, Theme.SP20);
            add(label, c);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(INK);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_SM, Theme.RADIUS_SM);
            g2.setColor(dot);
            g2.fillOval(Theme.SP12, getHeight() / 2 - 4, 8, 8);
            g2.dispose();
            super.paintChildren(g);
        }
    }
}
