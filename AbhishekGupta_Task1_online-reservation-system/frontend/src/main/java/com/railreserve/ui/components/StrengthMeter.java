package com.railreserve.ui.components;

import com.railreserve.service.SignupValidator;
import com.railreserve.service.SignupValidator.Strength;
import com.railreserve.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Password strength indicator: 4 segments (weak → strong) plus a plain-language
 * hint. Score comes from {@link SignupValidator#strength} so UI and service
 * agree on the same rules.
 */
public class StrengthMeter extends JComponent {

    private Strength strength = Strength.WEAK;

    public StrengthMeter() {
        setOpaque(false);
        setPreferredSize(new Dimension(330, 34));
        setMinimumSize(new Dimension(330, 34));
    }

    /** Updates the fill level and hint; repaints only what changed. */
    public void setStrength(Strength strength) {
        this.strength = strength;
        repaint();
        revalidate();
    }

    public Strength strength() {
        return strength;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int segments = 4;
        int barHeight = 6;
        int gap = Theme.SP4;
        int segWidth = (getWidth() - gap * (segments - 1)) / segments;

        Color on = switch (strength) {
            case WEAK -> Theme.ERROR;
            case FAIR -> Theme.ACCENT;
            case GOOD -> new Color(0x3F8F5F);
            case STRONG -> Theme.SUCCESS;
        };
        int lit = switch (strength) {
            case WEAK -> 1;
            case FAIR -> 2;
            case GOOD -> 3;
            case STRONG -> 4;
        };

        for (int i = 0; i < segments; i++) {
            g2.setColor(i < lit ? on : Theme.SURFACE_ALT);
            g2.fillRoundRect(i * (segWidth + gap), 0, segWidth, barHeight, barHeight, barHeight);
        }

        g2.setColor(Theme.MUTED);
        g2.setFont(Theme.SMALL.deriveFont(Font.PLAIN, 11f));
        g2.drawString(SignupValidator.strengthLabel(strength), 0, barHeight + Theme.SP12);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(330, 34);
    }
}
