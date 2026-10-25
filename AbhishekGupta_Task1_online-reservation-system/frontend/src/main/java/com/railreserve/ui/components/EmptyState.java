package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Dimension;

/**
 * Friendly empty state: soft icon badge, one-line "why" message and an
 * optional call-to-action underneath. Used wherever a list/table can come up
 * empty (overview, bookings, PNR lookups).
 */
public class EmptyState extends JPanel {

    private final Box column = new Box(BoxLayout.Y_AXIS);

    public EmptyState(Icon icon, String title, String subtitle) {
        this(icon, title, subtitle, null);
    }

    public EmptyState(Icon icon, String title, String subtitle, JComponent action) {
        super();
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP16, Theme.SP24, Theme.SP16));
        setLayout(new java.awt.GridBagLayout());
        column.setOpaque(false);

        JLabel badge = new JLabel(icon, SwingConstants.CENTER);
        badge.setAlignmentX(0.5f);
        column.add(badge);
        column.add(Box.createVerticalStrut(Theme.SP12));

        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(Theme.H3.deriveFont(java.awt.Font.BOLD, 15f));
        t.setForeground(Theme.TEXT);
        t.setAlignmentX(0.5f);
        column.add(t);
        column.add(Box.createVerticalStrut(Theme.SP4));

        JLabel s = new JLabel("<html><body style='width:420px;text-align:center'>" + subtitle + "</body></html>", SwingConstants.CENTER);
        s.setFont(Theme.SMALL);
        s.setForeground(Theme.MUTED);
        s.setAlignmentX(0.5f);
        column.add(s);

        if (action != null) {
            action.setFont(Theme.BODY);
            column.add(Box.createVerticalStrut(Theme.SP16));
            action.setAlignmentX(0.5f);
            column.add(action);
        }
        add(column);
        setPreferredSize(new Dimension(480, 220));
    }

    /** Neatly-dressed centered box version for tables (padded card body). */
    public JComponent boxed() {
        JPanel wrap = new Card(new java.awt.BorderLayout());
        wrap.add(this, java.awt.BorderLayout.CENTER);
        wrap.setPreferredSize(new Dimension(640, 240));
        return wrap;
    }
}
