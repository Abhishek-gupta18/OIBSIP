package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Color;

/**
 * White rounded surface used everywhere for grouped content: hairline border,
 * a barely-there shadow and 16px corners (the ixigo card look).
 */
public class Card extends JPanel {

    private static final Color SHADOW_LIGHT = new Color(15, 23, 42, 10);
    private static final Color SHADOW_DARK = new Color(0, 0, 0, 70);

    private final boolean painted;

    public Card(LayoutManager layout) {
        this(layout, true);
    }

    public Card(LayoutManager layout, boolean painted) {
        super(layout);
        this.painted = painted;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Theme.SP16, Theme.SP24, Theme.SP16, Theme.SP24));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!painted) {
            super.paintComponent(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int body = Math.max(0, h - 3);
        int arc = Theme.RADIUS;

        g2.setColor(Theme.isDark() ? SHADOW_DARK : SHADOW_LIGHT);
        g2.fillRoundRect(0, 3, w - 1, Math.max(0, body), arc, arc);

        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(0, 0, w - 1, body, arc, arc);

        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, body, arc, arc);
        g2.dispose();

        super.paintComponent(g);
    }
}
