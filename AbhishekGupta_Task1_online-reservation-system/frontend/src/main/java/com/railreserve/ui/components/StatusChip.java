package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Soft status badge: CONFIRMED = green on mint, CANCELLED = red on blush,
 * anything else = muted grey. Small dot + uppercase label, like ixigo's chips.
 */
public class StatusChip extends JPanel {

    private final JLabel label = new JLabel();
    private Color fill;
    private Color accent;

    public StatusChip(String status) {
        super(new GridBagLayout());
        setOpaque(false);
        label.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11f));
        label.setIconTextGap(Theme.SP8);
        ((GridBagLayout) getLayout()).setConstraints(label,
                new java.awt.GridBagConstraints(
                        0, 0, 1, 1, 0, 0, java.awt.GridBagConstraints.CENTER,
                        java.awt.GridBagConstraints.NONE,
                        new Insets(Theme.SP4, Theme.SP12, Theme.SP4, Theme.SP12), 0, 0));
        add(label);
        setStatus(status);
    }

    /** Re-renders the pill for the given reservation status (null-safe). */
    public void setStatus(String status) {
        String shown = status == null || status.isBlank() ? "—" : status.toUpperCase();
        switch (shown) {
            case "CONFIRMED" -> {
                fill = Theme.SUCCESS_SOFT;
                accent = Theme.SUCCESS;
            }
            case "CANCELLED" -> {
                fill = Theme.ERROR_SOFT;
                accent = Theme.ERROR;
            }
            default -> {
                fill = Theme.SURFACE_ALT;
                accent = Theme.MUTED;
            }
        }
        label.setText(shown);
        label.setForeground(accent);
        label.setIcon(new DotIcon(accent));
        repaint();
    }

    /** 6px filled circle shown before the chip text. */
    private static final class DotIcon implements Icon {
        private final Color color;

        DotIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y + 3, 6, 6);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 6;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill == null ? Theme.SURFACE_ALT : fill);
        int h = getHeight();
        g2.fillRoundRect(0, 0, getWidth(), h, h, h);
        g2.dispose();
        super.paintComponent(g);
    }
}
