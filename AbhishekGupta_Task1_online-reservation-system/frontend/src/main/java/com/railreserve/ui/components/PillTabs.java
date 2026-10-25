package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Segmented pill tabs — the row above ixigo's search card. The selected tab is
 * a solid orange pill; the rest are quiet grey text on the bar.
 */
public class PillTabs extends JPanel {

    private final List<JButton> tabs = new ArrayList<>();
    private final IntConsumer onSelect;
    private int selected;

    public PillTabs(List<String> labels, IntConsumer onSelect) {
        super(new FlowLayout(FlowLayout.LEFT, Theme.SP4, Theme.SP4));
        this.onSelect = onSelect;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, Theme.SP4, 0, Theme.SP4));
        for (int i = 0; i < labels.size(); i++) {
            JButton tab = new TabButton(labels.get(i), i);
            tabs.add(tab);
            add(tab);
        }
        select(0);
    }

    /** Programmatic selection (also fires the listener). */
    public void select(int index) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }
        selected = index;
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).setSelected(i == index);
            tabs.get(i).repaint();
        }
        if (onSelect != null) {
            onSelect.accept(index);
        }
    }

    public int selectedIndex() {
        return selected;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.SURFACE_ALT);
        int h = getHeight();
        g2.fillRoundRect(0, 0, getWidth(), h, h, h);
        g2.dispose();
        super.paintComponent(g);
    }

    /** One tab: rounded pill, orange when active. */
    private final class TabButton extends JButton {
        private final int index;

        TabButton(String text, int index) {
            super(text);
            this.index = index;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Theme.HAND);
            setFont(Theme.BODY.deriveFont(java.awt.Font.BOLD, 13f));
            setBorder(BorderFactory.createEmptyBorder(Theme.SP8, Theme.SP16, Theme.SP8, Theme.SP16));
            addActionListener(e -> PillTabs.this.select(index));
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            setForeground(selected ? Theme.ON_PRIMARY : Theme.MUTED);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isSelected()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? Theme.BRAND_HOVER : Theme.BRAND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
