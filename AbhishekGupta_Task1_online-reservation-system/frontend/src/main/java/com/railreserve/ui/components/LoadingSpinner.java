package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;

/** Small animated arc spinner for loading states (all DB calls are off the EDT). */
public class LoadingSpinner extends JComponent {

    private final int size;
    private int angle = 0;
    private final Timer timer;

    public LoadingSpinner(int size) {
        this.size = size;
        setPreferredSize(new Dimension(size, size));
        setOpaque(false);
        timer = new Timer(60, e -> {
            angle = (angle + 22) % 360;
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.ACCENT);
        g2.setStroke(new BasicStroke(Math.max(3, size / 12f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Double(2, 2, size - 4, size - 4, angle, 90, Arc2D.OPEN));
        g2.dispose();
    }
}
