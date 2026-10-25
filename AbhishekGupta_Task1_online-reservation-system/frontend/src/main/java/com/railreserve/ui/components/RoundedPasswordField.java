package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A rounded password input with a show/hide eye toggle, so the password
 * never leaves the JPasswordField contract while remaining checkable.
 */
public class RoundedPasswordField extends JPanel {

    private final RoundedBorder border;
    private final JPasswordField field = new JPasswordField();
    private final JButton toggle = new JButton();
    private final List<Runnable> changeListeners = new ArrayList<>();

    public RoundedPasswordField(int columns) {
        super(new BorderLayout());
        setOpaque(false);
        field.setColumns(columns);
        field.setFont(Theme.BODY);
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.BRAND);
        field.setBackground(Theme.SURFACE);
        field.setOpaque(true);
        field.setBorder(BorderFactory.createEmptyBorder(Theme.SP8, Theme.SP12, Theme.SP8, Theme.SP2));
        field.setEchoChar('•');

        toggle.setIcon(Icons.eye(16, false));
        toggle.setToolTipText("Show / hide password");
        toggle.setFocusPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setCursor(Theme.HAND);
        toggle.setForeground(Theme.MUTED);
        toggle.addActionListener(e -> {
            boolean showing = field.getEchoChar() == '\u0000';
            field.setEchoChar(showing ? '•' : '\u0000');
            toggle.setIcon(Icons.eye(16, !showing));
        });

        border = new RoundedBorder(Theme.RADIUS_SM, Theme.BORDER, 1);
        setOpaque(true);
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(border,
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        setPreferredSize(new java.awt.Dimension(field.getPreferredSize().width + 40, 36));
        add(field, BorderLayout.CENTER);
        add(toggle, BorderLayout.EAST);

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                border.setColor(Theme.BRAND);
                repaint();
            }

            @Override public void focusLost(java.awt.event.FocusEvent e) {
                border.setColor(Theme.BORDER);
                repaint();
            }
        });
    }

    /** Fires when the user presses Enter inside the field. */
    public void onEnter(java.awt.event.ActionListener listener) {
        field.addActionListener(listener);
    }

    /** Type-driven notifications; error border clears automatically when typing. */
    public void onTextChange(Runnable listener) {
        changeListeners.add(listener);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { fire(); }
            @Override public void removeUpdate(DocumentEvent e) { fire(); }
            @Override public void changedUpdate(DocumentEvent e) { fire(); }
        });
    }

    private void fire() {
        border.setColor(field.hasFocus() ? Theme.BRAND : Theme.BORDER);
        repaint();
        for (Runnable listener : List.copyOf(changeListeners)) {
            listener.run();
        }
    }

    public char[] getPassword() {
        return field.getPassword();
    }

    public String getTextContent() {
        return new String(field.getPassword());
    }

    public void setTextContent(String value) {
        field.setText(value);
    }

    public void clear() {
        field.setText("");
    }

    public void markError() {
        border.setColor(Theme.ERROR);
        repaint();
    }
}
