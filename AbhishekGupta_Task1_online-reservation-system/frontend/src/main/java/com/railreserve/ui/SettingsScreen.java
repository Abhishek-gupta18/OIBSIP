package com.railreserve.ui;

import com.railreserve.model.User;
import com.railreserve.service.AccountService;
import com.railreserve.service.PreferencesService;
import com.railreserve.service.ReservationService;
import com.railreserve.service.SignupValidator;
import com.railreserve.ui.components.Card;
import com.railreserve.ui.components.FormLabel;
import com.railreserve.ui.components.PrimaryButton;
import com.railreserve.ui.components.RoundedPasswordField;
import com.railreserve.ui.components.RoundedTextField;
import com.railreserve.ui.components.SecondaryButton;
import com.railreserve.ui.components.StrengthMeter;
import com.railreserve.ui.components.Toast;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Screen 5 — Settings: profile edit, change password (current verified),
 * preferences (live light/dark theme, default class + source, saved to a local
 * file) and a danger zone that deletes the account and its bookings after a
 * typed confirmation and password re-entry.
 */
public class SettingsScreen extends JPanel {

    private final AccountService accountService = new AccountService();
    private final PreferencesService preferencesService = new PreferencesService();
    private final Runnable onBack;
    private final Runnable onDeleted;

    // profile
    private final RoundedTextField fullName = new RoundedTextField(18);
    private final RoundedTextField email = new RoundedTextField(18);
    private final RoundedTextField phone = new RoundedTextField(18);
    private final FormLabel fullNameRow = new FormLabel("Full name");
    private final FormLabel emailRow = new FormLabel("E-mail");
    private final FormLabel phoneRow = new FormLabel("Phone (optional)");
    private final PrimaryButton saveProfile = new PrimaryButton("Save profile");

    // security
    private final RoundedPasswordField currentPassword = new RoundedPasswordField(16);
    private final RoundedPasswordField newPassword = new RoundedPasswordField(16);
    private final RoundedPasswordField confirmPassword = new RoundedPasswordField(16);
    private final FormLabel currentRow = new FormLabel("Current password");
    private final FormLabel newRow = new FormLabel("New password");
    private final FormLabel confirmRow = new FormLabel("Confirm new password");
    private final StrengthMeter meter = new StrengthMeter();
    private final PrimaryButton savePassword = new PrimaryButton("Update password");

    // preferences
    private final JComboBox<String> themeCombo = new JComboBox<>(new String[]{"Light", "Dark"});
    private final JComboBox<String> classCombo = new JComboBox<>(ReservationService.CLASS_TYPES);
    private final RoundedTextField sourceField = new RoundedTextField(14);
    private final PrimaryButton savePreferences = new PrimaryButton("Save preferences");

    // danger
    private final RoundedPasswordField deletePassword = new RoundedPasswordField(16);
    private final RoundedTextField deleteWord = new RoundedTextField(10);
    private final PrimaryButton deleteAccount = PrimaryButton.danger("Delete account permanently");

    public SettingsScreen(Runnable onBack, Runnable onDeleted) {
        this.onBack = onBack;
        this.onDeleted = onDeleted;

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        add(header(), BorderLayout.NORTH);
        add(scrollBody(), BorderLayout.CENTER);

        prefill();
        wire();
    }

    private JComponent header() {
        JPanel bar = new JPanel(new BorderLayout(Theme.SP16, 0));
        bar.setBackground(Theme.BACKGROUND);
        bar.setBorder(BorderFactory.createEmptyBorder(Theme.SP16, Theme.SP32, Theme.SP8, Theme.SP32));
        SecondaryButton back = SecondaryButton.link("\u2190 Back to dashboard");
        back.addActionListener(e -> onBack.run());
        bar.add(back, BorderLayout.WEST);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        JLabel title = new JLabel("Settings");
        title.setFont(Theme.H1);
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(0f);
        JLabel sub = new JLabel("Profile, security, preferences and account deletion.");
        sub.setFont(Theme.BODY);
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(0f);
        titles.add(title);
        titles.add(sub);
        bar.add(titles, BorderLayout.CENTER);
        return bar;
    }

    private JComponent scrollBody() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBackground(Theme.BACKGROUND);
        column.setBorder(BorderFactory.createEmptyBorder(Theme.SP8, Theme.SP32, Theme.SP24, Theme.SP32));

        column.add(profileCard());
        column.add(Box.createVerticalStrut(Theme.SP16));
        column.add(securityCard());
        column.add(Box.createVerticalStrut(Theme.SP16));
        column.add(preferencesCard());
        column.add(Box.createVerticalStrut(Theme.SP16));
        column.add(dangerCard());
        column.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(column,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private void prefill() {
        User user = UserSession.currentUser();
        if (user != null) {
            fullName.setText(user.getFullName() == null ? "" : user.getFullName());
            email.setText(user.getEmail() == null ? "" : user.getEmail());
            phone.setText(user.getPhone() == null ? "" : user.getPhone());
        }
        PreferencesService.Prefs prefs = preferencesService.load();
        themeCombo.setSelectedItem(prefs.darkMode() ? "Dark" : "Light");
        classCombo.setSelectedItem(prefs.defaultClass());
        sourceField.setText(prefs.defaultSource());
    }

    private void wire() {
        newPassword.onTextChange(() -> {
            meter.setStrength(SignupValidator.strength(newPassword.getTextContent()));
            confirmRow.setError(SignupValidator.mismatchError(
                    newPassword.getTextContent(), confirmPassword.getTextContent()));
        });
        confirmPassword.onTextChange(() -> confirmRow.setError(SignupValidator.mismatchError(
                newPassword.getTextContent(), confirmPassword.getTextContent())));

        saveProfile.addActionListener(e -> saveProfile());
        savePassword.addActionListener(e -> savePasswordChange());
        savePreferences.addActionListener(e -> savePrefs());
        deleteAccount.addActionListener(e -> deleteAccountWithConfirm());

        // theme applies the moment a value is picked — no save click needed
        themeCombo.addItemListener(e -> {
            if (java.awt.event.ItemEvent.SELECTED == e.getStateChange()) {
                applyTheme("Dark".equals(themeCombo.getSelectedItem()));
            }
        });
    }

    /**
     * Applies (and persists) the theme selection immediately; the shell then
     * rebuilds the screen, so the combo pre-fills from the saved file.
     */
    private void applyTheme(boolean dark) {
        if (Theme.isDark() == dark) {
            return;
        }
        try {
            preferencesService.save(new PreferencesService.Prefs(dark,
                    String.valueOf(classCombo.getSelectedItem()), sourceField.getText()));
        } catch (IOException ex) {
            Toast.show(this, "Could not save the theme to disk — it stays live until you restart.",
                    Toast.Kind.ERROR);
        }
        Theme.setDark(dark);
        // the rebuild replaced this screen; toast on the window's own layer
        AppFrame appFrame = AppFrame.instance();
        if (appFrame != null) {
            Toast.show(appFrame.layeredPane(),
                    dark ? "Dark theme applied." : "Light theme applied.", Toast.Kind.INFO);
        }
    }

    // ---------- section cards ----------

    private Card sectionCard(String title, String subtitle) {
        Card card = new Card(new GridBagLayout());
        card.setAlignmentX(0f);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setBorder(BorderFactory.createEmptyBorder(Theme.SP16, Theme.SP24, Theme.SP16, Theme.SP24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        JLabel t = new JLabel(title);
        t.setFont(Theme.H2);
        t.setForeground(Theme.TEXT);
        card.add(t, c);
        c.gridy = 1;
        c.insets = new Insets(Theme.SP2, 0, Theme.SP8, 0);
        JLabel s = new JLabel(subtitle);
        s.setFont(Theme.SMALL);
        s.setForeground(Theme.MUTED);
        card.add(s, c);
        return card;
    }

    private JComponent profileCard() {
        Card card = sectionCard("Profile", "Update the name and contact details shown across the app.");
        GridBagConstraints c = row(2);
        card.add(field(fullNameRow, fullName), c);
        c = row(3);
        card.add(field(emailRow, email), c);
        c = row(4);
        card.add(field(phoneRow, phone), c);
        c = row(5);
        c.insets = new Insets(Theme.SP12, 0, 0, 0);
        card.add(saveProfile, c);
        return card;
    }

    private JComponent securityCard() {
        Card card = sectionCard("Security", "Changing the password verifies your current one first.");
        GridBagConstraints c = row(2);
        card.add(field(currentRow, currentPassword), c);
        c = row(3);
        JPanel newBox = new JPanel(new BorderLayout(0, Theme.SP2));
        newBox.setOpaque(false);
        newBox.add(newRow, BorderLayout.NORTH);
        newBox.add(newPassword, BorderLayout.CENTER);
        card.add(newBox, c);
        c = row(4);
        c.insets = new Insets(Theme.SP4, 0, 0, 0);
        card.add(meter, c);
        c = row(5);
        card.add(field(confirmRow, confirmPassword), c);
        c = row(6);
        c.insets = new Insets(Theme.SP12, 0, 0, 0);
        card.add(savePassword, c);
        return card;
    }

    private JComponent preferencesCard() {
        Card card = sectionCard("Preferences",
                "Theme applies instantly app-wide; defaults pre-fill the booking form.");
        GridBagConstraints c = row(2);
        card.add(labeled("Theme", themeCombo), c);
        c = row(3);
        card.add(labeled("Default travel class", classCombo), c);
        c = row(4);
        card.add(labeled("Default source station", sourceField), c);
        c = row(5);
        c.insets = new Insets(Theme.SP12, 0, 0, 0);
        card.add(savePreferences, c);
        return card;
    }

    private JComponent dangerCard() {
        Card card = sectionCard("Danger zone",
                "Deleting your account also removes all your bookings in this demo. This cannot be undone.");
        GridBagConstraints c = row(2);
        card.add(field(new FormLabel("Password"), deletePassword), c);
        c = row(3);
        card.add(field(new FormLabel("Type DELETE to confirm"), deleteWord), c);
        c = row(4);
        c.insets = new Insets(Theme.SP12, 0, 0, 0);
        card.add(deleteAccount, c);
        return card;
    }

    private GridBagConstraints row(int gridy) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = gridy;
        c.gridwidth = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(Theme.SP4, 0, 0, 0);
        return c;
    }

    private JComponent field(FormLabel label, JComponent control) {
        JPanel box = new JPanel(new BorderLayout(0, Theme.SP2));
        box.setOpaque(false);
        box.add(label, BorderLayout.NORTH);
        control.setPreferredSize(new Dimension(360, 34));
        box.add(control, BorderLayout.CENTER);
        return box;
    }

    private JComponent labeled(String caption, JComponent control) {
        return field(new FormLabel(caption), control);
    }

    // ---------- actions ----------

    private void saveProfile() {
        boolean valid = true;
        valid &= inline(fullNameRow, SignupValidator.fullNameError(fullName.getText()));
        valid &= inline(emailRow, SignupValidator.emailError(email.getText()));
        valid &= inline(phoneRow, SignupValidator.phoneError(phone.getText()));
        if (!valid) {
            return;
        }
        saveProfile.setBusy(true);
        new SwingWorker<AccountService.ProfileOutcome, Void>() {
            @Override
            protected AccountService.ProfileOutcome doInBackground() {
                return accountService.updateProfile(UserSession.currentUser().getId(),
                        fullName.getText(), email.getText(), phone.getText());
            }

            @Override
            protected void done() {
                saveProfile.setBusy(false);
                try {
                    AccountService.ProfileOutcome outcome = get();
                    if (outcome.success()) {
                        User old = UserSession.currentUser();
                        UserSession.login(new User(old.getId(), old.getUsername(),
                                fullName.getText().strip(), email.getText().strip(),
                                phone.getText().isBlank() ? null : phone.getText().strip()));
                        Toast.show(SettingsScreen.this, outcome.message(), Toast.Kind.SUCCESS);
                    } else {
                        if (outcome.errors() != null) {
                            fullNameRow.setError(outcome.errors().fullName());
                            emailRow.setError(outcome.errors().email());
                            phoneRow.setError(outcome.errors().phone());
                        }
                        Toast.show(SettingsScreen.this, outcome.message(), Toast.Kind.ERROR);
                    }
                } catch (Exception ex) {
                    Toast.show(SettingsScreen.this, "Could not save the profile — please retry.", Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    private void savePasswordChange() {
        boolean valid = true;
        valid &= inline(currentRow, currentPassword.getTextContent().isBlank()
                ? "Enter your current password." : null);
        valid &= inline(newRow, SignupValidator.passwordError(newPassword.getTextContent()));
        valid &= inline(confirmRow, SignupValidator.mismatchError(
                newPassword.getTextContent(), confirmPassword.getTextContent()));
        if (!valid) {
            return;
        }
        savePassword.setBusy(true);
        new SwingWorker<AccountService.PasswordOutcome, Void>() {
            @Override
            protected AccountService.PasswordOutcome doInBackground() {
                return accountService.changePassword(UserSession.currentUser().getId(),
                        currentPassword.getTextContent(),
                        newPassword.getTextContent(), confirmPassword.getTextContent());
            }

            @Override
            protected void done() {
                savePassword.setBusy(false);
                try {
                    AccountService.PasswordOutcome outcome = get();
                    if (outcome.success()) {
                        currentPassword.clear();
                        newPassword.clear();
                        confirmPassword.clear();
                        meter.setStrength(SignupValidator.Strength.WEAK);
                        Toast.show(SettingsScreen.this, outcome.message(), Toast.Kind.SUCCESS);
                    } else {
                        if (outcome.errors() != null) {
                            newRow.setError(outcome.errors().password());
                            confirmRow.setError(outcome.errors().confirm());
                        }
                        Toast.show(SettingsScreen.this, outcome.message(), Toast.Kind.ERROR);
                    }
                } catch (Exception ex) {
                    Toast.show(SettingsScreen.this, "Could not change the password — please retry.",
                            Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    private void savePrefs() {
        boolean dark = "Dark".equals(themeCombo.getSelectedItem());
        PreferencesService.Prefs prefs = new PreferencesService.Prefs(dark,
                String.valueOf(classCombo.getSelectedItem()), sourceField.getText());
        try {
            preferencesService.save(prefs);
            applyTheme(dark);   // no-op when already live; rebuilds the screen otherwise
        } catch (IOException ex) {
            Toast.show(this,
                    "Could not save preferences to the local file — check folder permissions.",
                    Toast.Kind.ERROR);
        }
    }

    private void deleteAccountWithConfirm() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete your account?\nAll your bookings will be removed too. This cannot be undone.",
                "Delete account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        deleteAccount.setBusy(true);
        new SwingWorker<AccountService.DeleteOutcome, Void>() {
            @Override
            protected AccountService.DeleteOutcome doInBackground() {
                return accountService.deleteAccount(UserSession.currentUser().getId(),
                        deletePassword.getTextContent(), deleteWord.getText());
            }

            @Override
            protected void done() {
                deleteAccount.setBusy(false);
                try {
                    AccountService.DeleteOutcome outcome = get();
                    if (outcome.success()) {
                        onDeleted.run();
                        Toast.show(SettingsScreen.this, outcome.message(), Toast.Kind.SUCCESS);
                    } else {
                        Toast.show(SettingsScreen.this, outcome.message(), Toast.Kind.ERROR);
                    }
                } catch (Exception ex) {
                    Toast.show(SettingsScreen.this, "Could not delete the account — please retry.",
                            Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    private static boolean inline(FormLabel row, String error) {
        row.setError(error);
        return error == null;
    }
}
