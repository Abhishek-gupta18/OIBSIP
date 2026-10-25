package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

/** Rounded single-line input; white surface, orange focus ring, red error state. */
public class RoundedTextField extends JTextField {

    private final RoundedBorder border;
    private final List<Runnable> changeListeners = new ArrayList<>();
    private boolean errorState;

    public RoundedTextField(int columns) {
        super(columns);
        setFont(Theme.BODY);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.BRAND);
        setBackground(Theme.SURFACE);
        setOpaque(true);
        border = new RoundedBorder(Theme.RADIUS_SM, Theme.BORDER, 1);
        setBorder(border);
        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { fireChange(); }
            @Override public void removeUpdate(DocumentEvent e) { fireChange(); }
            @Override public void changedUpdate(DocumentEvent e) { fireChange(); }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { paintBorderState(true); }
            @Override public void focusLost(FocusEvent e) { paintBorderState(false); }
        });
    }

    private void paintBorderState(boolean focused) {
        border.setColor(errorState ? Theme.ERROR : focused ? Theme.BRAND : Theme.BORDER);
        repaint();
    }

    private void fireChange() {
        setErrorState(false);
        for (Runnable listener : List.copyOf(changeListeners)) {
            listener.run();
        }
    }

    /** Marks the field invalid (red border); auto-clears when the user types. */
    public void setErrorState(boolean error) {
        errorState = error;
        border.setColor(error ? Theme.ERROR : hasFocus() ? Theme.BRAND : Theme.BORDER);
        repaint();
    }

    /** Type-driven notifications: cleared automatically when text changes. */
    public void onTextChange(Runnable listener) {
        changeListeners.add(listener);
    }
}
