package com.railreserve.ui;

import com.railreserve.service.AuthService;
import com.railreserve.service.SignupValidator;
import com.railreserve.ui.components.Card;
import com.railreserve.ui.components.FormLabel;
import com.railreserve.ui.components.Icons;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

/**
 * Screen 2 — Sign Up: live per-field inline validation, password strength
 * meter, friendly duplicate handling (service layer), PBKDF2 hashing. On
 * success: toast + redirect to Login (F4).
 */
public class SignUpScreen extends JPanel {

    private final AuthService authService = new AuthService(new com.railreserve.dao.UserDAO());
    private final Consumer<String> onSuccess;
    private final Runnable onGoLogin;

    private final RoundedTextField fullName = new RoundedTextField(18);
    private final RoundedTextField usernameField = new RoundedTextField(18);
    private final RoundedTextField emailField = new RoundedTextField(18);
    private final RoundedTextField phoneField = new RoundedTextField(18);
    private final RoundedPasswordField password = new RoundedPasswordField(18);
    private final RoundedPasswordField confirm = new RoundedPasswordField(18);
    private final StrengthMeter strengthMeter = new StrengthMeter();
    private final PrimaryButton createAccount = new PrimaryButton("Create account");

    private final FormLabel nameRow = new FormLabel("Full name");
    private final FormLabel usernameRow = new FormLabel("Username");
    private final FormLabel emailRow = new FormLabel("E-mail");
    private final FormLabel phoneRow = new FormLabel("Phone (optional)");
    private final FormLabel passwordRow = new FormLabel("Password");
    private final FormLabel confirmRow = new FormLabel("Confirm password");

    public SignUpScreen(Consumer<String> onSuccess, Runnable onGoLogin) {
        this.onSuccess = onSuccess;
        this.onGoLogin = onGoLogin;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        add(buildContent(), BorderLayout.CENTER);
        wireLiveValidation();
    }

    private JComponent buildContent() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Theme.BACKGROUND);

        Card card = new Card(new GridBagLayout());
        card.setPreferredSize(new Dimension(780, 570));
        card.setBorder(BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP32, Theme.SP24, Theme.SP32));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;

        card.add(brandHeader(), c);

        c.gridy++;
        c.gridwidth = 1;
        c.insets = new Insets(Theme.SP16, 0, 0, Theme.SP8);
        card.add(field(nameRow, fullName, 320), c);
        c.gridx = 1;
        c.insets = new Insets(Theme.SP16, Theme.SP8, 0, 0);
        card.add(field(usernameRow, usernameField, 320), c);

        c.gridy++;
        c.gridx = 0;
        c.insets = new Insets(Theme.SP12, 0, 0, Theme.SP8);
        card.add(field(emailRow, emailField, 320), c);
        c.gridx = 1;
        c.insets = new Insets(Theme.SP12, Theme.SP8, 0, 0);
        card.add(field(phoneRow, phoneField, 320), c);

        c.gridy++;
        c.gridx = 0;
        c.insets = new Insets(Theme.SP12, 0, 0, Theme.SP8);
        card.add(passwordColumn(), c);
        c.gridx = 1;
        c.insets = new Insets(Theme.SP12, Theme.SP8, 0, 0);
        card.add(field(confirmRow, confirm, 320), c);

        createAccount.addActionListener(e -> submit());
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(Theme.SP16, 0, 0, 0);
        createAccount.setPreferredSize(new Dimension(0, 46));
        card.add(createAccount, c);

        c.gridy++;
        c.insets = new Insets(Theme.SP12, 0, 0, 0);
        SecondaryButton loginLink = SecondaryButton.link("Already have an account? Login");
        loginLink.addActionListener(e -> onGoLogin.run());
        card.add(loginLink, c);

        outer.add(card, new GridBagConstraints());
        return outer;
    }

    /** Orange badge, heading and sub-line above the form grid. */
    private JComponent brandHeader() {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);

        JLabel icon = new JLabel(Icons.train(26));
        icon.setAlignmentX(0f);
        block.add(icon);
        block.add(Box.createVerticalStrut(Theme.SP8));

        JLabel title = new JLabel("Create your RailReserve account");
        title.setFont(Theme.H1.deriveFont(26f));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(0f);
        block.add(title);
        block.add(Box.createVerticalStrut(Theme.SP4));

        JLabel sub = new JLabel("It takes a minute — and your bookings stay in one place.");
        sub.setFont(Theme.BODY);
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(0f);
        block.add(sub);
        return block;
    }

    /** Generic field: FormLabel with inline error + control underneath. */
    private JComponent field(FormLabel label, JComponent control, int width) {
        JPanel box = new JPanel(new BorderLayout(0, Theme.SP4));
        box.setOpaque(false);
        box.add(label, BorderLayout.NORTH);
        control.setPreferredSize(new Dimension(width, 42));
        box.add(control, BorderLayout.CENTER);
        return box;
    }

    /** Password column: label + field on top, strength meter below. */
    private JComponent passwordColumn() {
        JComponent f = field(passwordRow, password, 320);
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        f.setAlignmentX(LEFT_ALIGNMENT);
        col.add(f);
        col.add(Box.createVerticalStrut(Theme.SP4));
        strengthMeter.setAlignmentX(LEFT_ALIGNMENT);
        col.add(strengthMeter);
        return col;
    }

    /** Clears inline errors as the user types; keeps meter in sync. */
    private void wireLiveValidation() {
        fullName.onTextChange(() -> nameRow.setError(SignupValidator.fullNameError(fullName.getText())));
        usernameField.onTextChange(() -> usernameRow.setError(SignupValidator.usernameError(usernameField.getText())));
        emailField.onTextChange(() -> emailRow.setError(SignupValidator.emailError(emailField.getText())));
        phoneField.onTextChange(() -> phoneRow.setError(SignupValidator.phoneError(phoneField.getText())));
        password.onTextChange(() -> {
            strengthMeter.setStrength(SignupValidator.strength(password.getTextContent()));
            confirmRow.setError(SignupValidator.mismatchError(
                    password.getTextContent(), confirm.getTextContent()));
        });
        confirm.onTextChange(() -> confirmRow.setError(SignupValidator.mismatchError(
                password.getTextContent(), confirm.getTextContent())));
    }

    private static boolean setInline(FormLabel row, String error) {
        row.setError(error);
        return error == null;
    }

    /** Edits are never logged; validation from the service, not twice. */
    private void submit() {
        String pwd = password.getTextContent();
        String pwConfirm = confirm.getTextContent();

        boolean valid = true;
        valid &= setInline(nameRow, SignupValidator.fullNameError(fullName.getText()));
        valid &= setInline(usernameRow, SignupValidator.usernameError(usernameField.getText()));
        valid &= setInline(emailRow, SignupValidator.emailError(emailField.getText()));
        valid &= setInline(phoneRow, SignupValidator.phoneError(phoneField.getText()));
        valid &= setInline(passwordRow, SignupValidator.passwordError(pwd));
        valid &= setInline(confirmRow, SignupValidator.mismatchError(pwd, pwConfirm));
        if (!valid) {
            Toast.show(this, "Please fix the highlighted fields.", Toast.Kind.ERROR);
            return;
        }

        createAccount.setBusy(true);
        new SwingWorker<AuthService.SignupOutcome, Void>() {
            @Override
            protected AuthService.SignupOutcome doInBackground() {
                return authService.signup(new AuthService.SignupRequest(
                        fullName.getText(), usernameField.getText(), emailField.getText(),
                        phoneField.getText(), pwd, pwConfirm));
            }

            @Override
            protected void done() {
                createAccount.setBusy(false);
                try {
                    AuthService.SignupOutcome outcome = get();
                    if (outcome.success()) {
                        onSuccess.accept(outcome.message());
                    } else {
                        applyErrors(outcome);
                    }
                } catch (Exception ex) {
                    Toast.show(SignUpScreen.this,
                            "Could not create the account right now — please try again.",
                            Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    private void applyErrors(AuthService.SignupOutcome outcome) {
        SignupValidator.FieldErrors errors = outcome.fieldErrors();
        if (errors != null) {
            nameRow.setError(errors.fullName());
            usernameRow.setError(errors.username());
            emailRow.setError(errors.email());
            phoneRow.setError(errors.phone());
            passwordRow.setError(errors.password());
            confirmRow.setError(errors.confirm());
        }
        Toast.show(this, outcome.message(), Toast.Kind.ERROR);
    }
}
