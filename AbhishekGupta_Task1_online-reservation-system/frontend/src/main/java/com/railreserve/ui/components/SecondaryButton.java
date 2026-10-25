package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.JButton;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Quiet button: white surface with a hairline border and ink label (ixigo's
 * "secondary" treatment). {@link #link(String)} is the borderless orange text
 * link used for footer links, FAQ rows and "already have an account".
 */
public class SecondaryButton extends JButton {

    private final boolean link;

    public SecondaryButton(String text) {
        this(text, false);
    }

    private SecondaryButton(String text, boolean link) {
        super(text);
        this.link = link;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(link ? Theme.BRAND : Theme.TEXT);
        setFont(link ? Theme.BODY.deriveFont(Font.PLAIN, 13.5f)
                : Theme.BODY.deriveFont(Font.PLAIN, 13.5f));
        setCursor(Theme.HAND);
        if (!link) {
            setBorder(javax.swing.BorderFactory.createEmptyBorder(Theme.SP8, Theme.SP16,
                    Theme.SP8, Theme.SP16));
        }
    }

    /** Borderless orange text link — no chrome, no padding. */
    public static SecondaryButton link(String text) {
        return new SecondaryButton(text, true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setForeground(enabled ? (link ? Theme.BRAND : Theme.TEXT) : Theme.MUTED);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!link) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = getModel().isRollover() && isEnabled();
            g2.setColor(hover ? Theme.BRAND_SOFT : Theme.SURFACE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_SM, Theme.RADIUS_SM);
            g2.setColor(hover ? Theme.BRAND : Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS_SM, Theme.RADIUS_SM);
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
