package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Solid brand-orange CTA (ixigo style): bold white label, 10px corners, darker
 * on hover. {@link #danger(String)} paints the same shape in red for
 * destructive actions.
 */
public class PrimaryButton extends JButton {

    private String baseText;
    private boolean busy;
    private boolean danger;

    public PrimaryButton(String text) {
        super(text);
        this.baseText = text;
        style();
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { repaint(); }
            @Override public void mouseExited(MouseEvent e) { repaint(); }
        });
    }

    /** Red variant used by the settings danger zone. */
    public static PrimaryButton danger(String text) {
        PrimaryButton button = new PrimaryButton(text);
        button.danger = true;
        return button;
    }

    private void style() {
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Theme.ON_PRIMARY);
        setFont(Theme.BODY.deriveFont(Font.BOLD, 13.5f));
        setCursor(Theme.HAND);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(Theme.SP8, Theme.SP16,
                Theme.SP8, Theme.SP16));
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        this.baseText = getText().replace(" …", "");
        setText(busy ? this.baseText + " …" : this.baseText);
        setEnabled(!busy);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean hover = getModel().isRollover() && isEnabled();
        Color base = danger ? Theme.ERROR : Theme.BRAND;
        Color fill = hover ? (danger ? Theme.ERROR.darker() : Theme.BRAND_HOVER) : base;
        if (!isEnabled()) {
            fill = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 110);
        }
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_SM, Theme.RADIUS_SM);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        if (!busy) {
            baseText = text;
        }
    }
}
