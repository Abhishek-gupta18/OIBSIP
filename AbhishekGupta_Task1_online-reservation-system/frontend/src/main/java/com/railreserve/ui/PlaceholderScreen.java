package com.railreserve.ui;

import com.railreserve.ui.components.SecondaryButton;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Interim placeholder for screens built in upcoming milestones (e.g. Login,
 * Sign Up, Dashboard). It links back to the landing page so navigation never
 * dead-ends during incremental development.
 */
public class PlaceholderScreen extends JPanel {

    public PlaceholderScreen(String title, String note) {
        setLayout(new BorderLayout(0, Theme.SP16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SP48, Theme.SP48, Theme.SP48, Theme.SP48));

        JLabel heading = new JLabel(title);
        heading.setFont(Theme.H1);
        heading.setForeground(Theme.PRIMARY);
        add(heading, BorderLayout.NORTH);

        JLabel body = new JLabel("<html><body style='width:520px'>" + note
                + "<br><br>This section ships with an upcoming milestone.</body></html>");
        body.setFont(Theme.BODY);
        body.setForeground(Theme.MUTED);
        add(body, BorderLayout.CENTER);

        JPanel backBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backBar.setOpaque(false);
        SecondaryButton back = new SecondaryButton("← Back to Home");
        back.addActionListener(e -> ((AppFrame) getTopLevelAncestor()).show(AppFrame.HOME));
        backBar.add(back);
        add(backBar, BorderLayout.SOUTH);
    }
}
