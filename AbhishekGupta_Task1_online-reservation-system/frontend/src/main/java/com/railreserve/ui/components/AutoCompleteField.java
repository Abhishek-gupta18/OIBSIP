package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Rounded text field with a filtered suggestion popup (station-search style).
 * Suggestions come from a supplier (up to 8 shown, contains-match, typed text
 * preferred-first). Popup items are click-only so keyboard focus stays in the
 * field; Enter (and any other key) still flows to the text field normally.
 */
public class AutoCompleteField extends RoundedTextField {

    private final SuggestionPopup popup = new SuggestionPopup();
    private final Consumer<String> submitAction;

    public AutoCompleteField(int columns, Supplier<List<String>> suggestions, Consumer<String> submitAction) {
        super(columns);
        this.submitAction = submitAction;
        onTextChange(() -> {
            String typed = getText().strip();
            if (typed.isEmpty()) {
                popup.hide();
                return;
            }
            List<String> ranked = suggestions.get().stream()
                    .filter(s -> s.toLowerCase().contains(typed.toLowerCase()))
                    .sorted((a, b) -> Boolean.compare(
                            b.toLowerCase().startsWith(typed.toLowerCase()),
                            a.toLowerCase().startsWith(typed.toLowerCase())))
                    .limit(8)
                    .toList();
            if (ranked.isEmpty()) {
                popup.hide();
            } else {
                popup.showSuggestions(ranked);
            }
        });
        // hide the popup whenever the caret leaves the field
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                hidePopup();
            }
        });
    }

    /** Fires the submit consumer with the current text (trim handled by caller logic). */
    public void submit() {
        submitAction.accept(getText().strip());
    }

    private void hidePopup() {
        popup.hide();
    }

    /** Lightweight popup listing candidate values; clicking one fills the field. */
    private final class SuggestionPopup extends JPopupMenu {

        void showSuggestions(List<String> values) {
            removeAll();
            setBorder(null);
            for (String value : values) {
                JMenuItem item = new JMenuItem(value);
                item.setFont(Theme.BODY);
                item.setForeground(Theme.TEXT);
                item.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        setText(value);
                        setCaretPosition(value.length());
                        hide();
                    }
                });
                add(item);
            }
            // popup width follows the field for a tidy docked look
            setPopupSize(Math.max(getWidth(), 220), values.size() * 26 + 6);
            setBordered();
            show(AutoCompleteField.this, 0, getHeight() == 0 ? 0 : AutoCompleteField.this.getHeight());
        }

        private void setBordered() {
            setBorder(javax.swing.BorderFactory.createLineBorder(Theme.MUTED));
        }
    }
}
