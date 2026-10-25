package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

/** Label + optional inline error line shown right under the field it belongs to. */
public class FormLabel extends JPanel {

    private final JLabel label = new JLabel();
    private final JLabel error = new JLabel(" ");

    public FormLabel(String text) {
        super(new BorderLayout());
        setOpaque(false);
        label.setText(text);
        label.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11.5f));
        label.setForeground(Theme.MUTED);
        error.setForeground(Theme.ERROR);
        error.setFont(Theme.SMALL.deriveFont(Font.PLAIN, 11.5f));
        add(label, BorderLayout.NORTH);
        add(error, BorderLayout.SOUTH);
    }

    /** Shows a red inline message, or clears it when null/empty. */
    public void setError(String message) {
        error.setText(message == null || message.isBlank() ? " " : message);
    }
}
