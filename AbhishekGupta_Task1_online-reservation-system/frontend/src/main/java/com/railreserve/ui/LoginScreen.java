package com.railreserve.ui;

import com.railreserve.service.AuthService;
import com.railreserve.service.LoginAttemptTracker;
import com.railreserve.ui.components.Card;
import com.railreserve.ui.components.FormLabel;
import com.railreserve.ui.components.Icons;
import com.railreserve.ui.components.PrimaryButton;
import com.railreserve.ui.components.RoundedPasswordField;
import com.railreserve.ui.components.RoundedTextField;
import com.railreserve.ui.components.SecondaryButton;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

/**
 * Screen 3 — Login: username or e-mail + password, show/hide toggle, Enter
 * submits, busy button state while checking. Brute-force protection lives in
 * {@link LoginAttemptTracker}: 5 failures → 30-second lockout with a live
 * countdown. Login runs on a SwingWorker; the UI never blocks.
 */
public class LoginScreen extends JPanel {

    private final AuthService authService = new AuthService(new com.railreserve.dao.UserDAO());
    private final LoginAttemptTracker tracker;
    private final Consumer<String> onSuccess;   // message shown after routing away
    private final Runnable onGoSignUp;

    private final RoundedTextField identity = new RoundedTextField(18);
    private final RoundedPasswordField password = new RoundedPasswordField(18);
    private final PrimaryButton signIn = new PrimaryButton("Sign in");
    private final FormLabel identityRow = new FormLabel("Username or e-mail");
    private final FormLabel passwordRow = new FormLabel("Password");
    private final JLabel lockNote = new JLabel(" ");
    private Timer lockTimer;

    public LoginScreen(LoginAttemptTracker tracker, Consumer<String> onSuccess, Runnable onGoSignUp) {
        this.tracker = tracker;
        this.onSuccess = onSuccess;
        this.onGoSignUp = onGoSignUp;

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        add(buildContent(), BorderLayout.CENTER);

        identity.addActionListener(e -> submit());
        password.onEnter(e -> submit());
        signIn.addActionListener(e -> submit());
        lockNote.setForeground(Theme.ERROR);
        lockNote.setFont(Theme.SMALL.deriveFont(Font.PLAIN, 12f));
    }

    private JComponent buildContent() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Theme.BACKGROUND);

        Card card = new Card(new GridBagLayout());
        card.setPreferredSize(new Dimension(460, 500));
        card.setBorder(BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP32, Theme.SP24, Theme.SP32));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;

        card.add(brandBlock("Welcome back", "Sign in to book and manage your tickets."), c);

        c.gridy++;
        c.insets = new Insets(Theme.SP20, 0, 0, 0);
        card.add(field(identityRow, identity), c);

        c.gridy++;
        c.insets = new Insets(Theme.SP12, 0, 0, 0);
        card.add(field(passwordRow, password), c);

        c.gridy++;
        c.insets = new Insets(Theme.SP8, 0, 0, 0);
        card.add(lockNote, c);

        c.gridy++;
        c.insets = new Insets(Theme.SP16, 0, 0, 0);
        signIn.setPreferredSize(new Dimension(0, 46));
        card.add(signIn, c);

        c.gridy++;
        c.insets = new Insets(Theme.SP12, 0, 0, 0);
        SecondaryButton signUpLink = SecondaryButton.link("New to RailReserve? Create an account");
        signUpLink.addActionListener(e -> onGoSignUp.run());
        card.add(signUpLink, c);

        c.gridy++;
        c.insets = new Insets(Theme.SP16, 0, 0, 0);
        card.add(demoHint(), c);

        GridBagConstraints cc = new GridBagConstraints();
        outer.add(card, cc);
        return outer;
    }

    /** Small orange badge + heading + sub-line, the ixigo auth-card header. */
    private JComponent brandBlock(String title, String subtitle) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);

        JPanel mark = new JPanel(new BorderLayout());
        mark.setOpaque(false);
        mark.setPreferredSize(new Dimension(46, 46));
        mark.setMaximumSize(new Dimension(46, 46));
        mark.setBorder(BorderFactory.createEmptyBorder(Theme.SP8, Theme.SP8, Theme.SP8, Theme.SP8));
        JLabel icon = new JLabel(Icons.train(26));
        mark.add(icon, BorderLayout.WEST);
        mark.setAlignmentX(0f);
        block.add(mark);
        block.add(Box.createVerticalStrut(Theme.SP12));

        JLabel heading = new JLabel(title);
        heading.setFont(Theme.H1.deriveFont(26f));
        heading.setForeground(Theme.TEXT);
        heading.setAlignmentX(0f);
        block.add(heading);
        block.add(Box.createVerticalStrut(Theme.SP4));

        JLabel sub = new JLabel(subtitle);
        sub.setFont(Theme.BODY);
        sub.setForeground(Theme.MUTED);
        sub.setAlignmentX(0f);
        block.add(sub);
        return block;
    }

    /** Demo credentials shown on a soft orange strip. */
    private JComponent demoHint() {
        JLabel hint = new JLabel("Demo account  ·  demo / demo123");
        hint.setFont(Theme.SMALL);
        hint.setForeground(Theme.BRAND);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(Theme.SP8, Theme.SP12,
                Theme.SP8, Theme.SP12));
        wrap.add(hint, BorderLayout.WEST);
        return new RoundedPanel(wrap);
    }

    private JComponent field(FormLabel label, JComponent control) {
        JPanel box = new JPanel(new BorderLayout(0, Theme.SP4));
        box.setOpaque(false);
        box.add(label, BorderLayout.NORTH);
        control.setPreferredSize(new Dimension(320, 42));
        box.add(control, BorderLayout.CENTER);
        return box;
    }

    private void submit() {
        tracker.resetIfLockExpired();
        if (tracker.isLocked()) {
            startLockCountdown();
            return;
        }
        String id = identity.getText().strip();
        String pwd = password.getTextContent();
        boolean ok = true;
        if (id.isEmpty()) {
            identityRow.setError("Enter your username or e-mail.");
            ok = false;
        }
        if (pwd.isEmpty()) {
            passwordRow.setError("Enter your password.");
            ok = false;
        }
        if (!ok) {
            return;
        }
        identityRow.setError(null);
        passwordRow.setError(null);

        signIn.setBusy(true);
        new SwingWorker<AuthService.LoginResult, Void>() {
            @Override
            protected AuthService.LoginResult doInBackground() {
                return authService.login(id, pwd);
            }

            @Override
            protected void done() {
                signIn.setBusy(false);
                try {
                    AuthService.LoginResult result = get();
                    if (result.success()) {
                        tracker.recordSuccess();
                        UserSession.login(result.user());
                        onSuccess.accept("Welcome back, " + UserSession.displayName() + "!");
                    } else {
                        tracker.recordFailure();
                        if (tracker.isLocked()) {
                            startLockCountdown();
                        } else {
                            passwordRow.setError(null);
                            lockNote.setText(result.errorMessage());
                            password.requestFocusInWindow();
                        }
                    }
                } catch (Exception ex) {
                    lockNote.setText("Sign-in is unavailable right now — please try again shortly.");
                }
            }
        }.execute();
    }

    private void startLockCountdown() {
        if (lockTimer != null) {
            lockTimer.stop();
        }
        lockTimer = new Timer(1000, (java.awt.event.ActionEvent e) -> {
            long remaining = tracker.remainingSeconds();
            if (remaining <= 0) {
                lockNote.setText("You can try signing in again.");
                ((Timer) e.getSource()).stop();
            } else {
                lockNote.setText("Too many attempts — locked for another " + remaining + "s.");
            }
        });
        long first = tracker.remainingSeconds();
        lockNote.setText("Too many attempts — locked for " + first + "s.");
        lockTimer.start();
    }

    /** Rounded background helper (soft brand strips). */
    private static final class RoundedPanel extends JPanel {
        RoundedPanel(JComponent inner) {
            super(new BorderLayout());
            setOpaque(false);
            add(inner, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.BRAND_SOFT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_SM, Theme.RADIUS_SM);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
