package com.railreserve.ui.landing;

import com.railreserve.model.Reservation;
import com.railreserve.service.ReservationService;
import com.railreserve.ui.SearchIntent;
import com.railreserve.ui.components.AutoCompleteField;
import com.railreserve.ui.components.Card;
import com.railreserve.ui.components.Icons;
import com.railreserve.ui.components.LoadingSpinner;
import com.railreserve.ui.components.NavBar;
import com.railreserve.ui.components.PillTabs;
import com.railreserve.ui.components.PrimaryButton;
import com.railreserve.ui.components.SecondaryButton;
import com.railreserve.ui.components.TicketCard;
import com.railreserve.ui.data.StationCatalog;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Screen 1 — public landing page (shown at startup), styled after ixigo.com:
 * white navbar, a soft orange hero band with a big rounded search card on top
 * (pill tabs for train search / PNR status), a trust strip, benefit cards,
 * popular routes, FAQ and a light footer.
 */
public class LandingPanel extends JPanel {

    private final Consumer<String> onNavLink;
    private final Consumer<SearchIntent> onSearch;
    private final Runnable onLogin;
    private final Runnable onSignUp;
    private final ReservationService reservationService = new ReservationService();

    private String sessionName;
    private Consumer<String> onSessionAction;

    private AutoCompleteField fromField;
    private AutoCompleteField toField;
    private JSpinner dateSpinner;
    private JComboBox<String> classCombo;
    private JTextField pnrField;
    private JPanel pnrResultSlot;
    private JLabel pnrHint;
    private JScrollPane scroll;

    /**
     * @param sessionName display name of the logged-in user, or null for a
     *                    public visitor; navbar right side adapts accordingly
     */
    public LandingPanel(Consumer<String> onNavLink, Consumer<SearchIntent> onSearch,
                        Runnable onLogin, Runnable onSignUp,
                        String sessionName, Consumer<String> onSessionAction) {
        this.onNavLink = onNavLink;
        this.onSearch = onSearch;
        this.onLogin = onLogin;
        this.onSignUp = onSignUp;
        this.sessionName = sessionName;
        this.onSessionAction = onSessionAction;

        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        add(nav(), BorderLayout.NORTH);

        JPanel sections = new SectionsPanel();
        sections.setLayout(new GridBagLayout());
        sections.setBackground(Theme.BACKGROUND);
        GridBagConstraints cell = new GridBagConstraints();
        cell.gridx = 0;
        cell.weightx = 1.0;
        cell.fill = GridBagConstraints.HORIZONTAL;
        cell.anchor = GridBagConstraints.NORTHWEST;
        sections.add(hero(), cell);
        sections.add(trustStrip(), cell);
        cell.insets = new Insets(Theme.SP32, 0, 0, 0);
        sections.add(whyCards(), cell);
        sections.add(howItWorks(), cell);
        sections.add(popularRoutes(), cell);
        sections.add(faq(), cell);
        sections.add(footer(), cell);

        JScrollPane scroll = new JScrollPane(sections,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        this.scroll = scroll;
        add(scroll, BorderLayout.CENTER);
    }

    /** The page must open at the very top, whatever grabs the initial focus. */
    @Override
    public void addNotify() {
        super.addNotify();
        javax.swing.SwingUtilities.invokeLater(() ->
                scroll.getViewport().setViewPosition(new java.awt.Point(0, 0)));
    }

    // ---------- top nav ----------

    private JComponent nav() {
        NavBar nav = new NavBar(List.of("Home", "Book Tickets", "PNR Status", "Cancel Ticket", "Help"),
                onNavLink);
        nav.setActive("Home");
        if (sessionName == null) {
            nav.addLoginActions(e -> onLogin.run(), e -> onSignUp.run());
        } else {
            nav.addSessionActions(sessionName, onSessionAction);
        }
        return nav;
    }

    // ---------- hero + search widget ----------

    private JComponent hero() {
        HeroPanel hero = new HeroPanel();
        hero.setLayout(new GridBagLayout());
        hero.setBorder(BorderFactory.createEmptyBorder(Theme.SP40, Theme.SP48, Theme.SP40, Theme.SP48));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;

        hero.add(eyebrow("INDIA'S TRAIN BOOKING DEMO"), c);

        JLabel headline = new JLabel("Book train tickets, minus the hassle");
        headline.setFont(Theme.H1.deriveFont(Font.BOLD, 38f));
        headline.setForeground(Theme.TEXT);
        headline.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy++;
        c.insets = new Insets(Theme.SP12, 0, 0, 0);
        hero.add(headline, c);

        JLabel tagline = new JLabel("Search a route, pick a train and get a 10-digit PNR in seconds.");
        tagline.setFont(Theme.BODY.deriveFont(16f));
        tagline.setForeground(Theme.MUTED);
        tagline.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy++;
        c.insets = new Insets(Theme.SP8, 0, 0, 0);
        hero.add(tagline, c);

        c.gridy++;
        c.insets = new Insets(Theme.SP24, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        hero.add(searchWidget(), c);
        return hero;
    }

    private JComponent eyebrow(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11f));
        label.setForeground(Theme.BRAND);
        label.setBorder(BorderFactory.createEmptyBorder(Theme.SP4, Theme.SP12,
                Theme.SP4, Theme.SP12));
        JPanel pill = new JPanel(new BorderLayout());
        pill.setOpaque(false);
        pill.add(label, BorderLayout.CENTER);
        pill.setBorder(BorderFactory.createLineBorder(Theme.BRAND_SOFT.darker(), 0));
        return new RoundedTint(pill);
    }

    /** Search card: pill tabs on top, train search or PNR status below. */
    private Card searchWidget() {
        Card card = new Card(new BorderLayout(0, Theme.SP12));
        card.setBorder(BorderFactory.createEmptyBorder(Theme.SP16, Theme.SP20,
                Theme.SP16, Theme.SP20));
        card.setMinimumSize(new Dimension(560, 0));

        JPanel body = new JPanel(new CardLayout());
        body.setOpaque(false);

        PillTabs tabs = new PillTabs(List.of("Train search", "PNR status"), index -> {
            ((CardLayout) body.getLayout()).show(body, index == 0 ? "train" : "pnr");
        });

        body.add(trainSearch(), "train");
        body.add(pnrSearch(), "pnr");
        ((CardLayout) body.getLayout()).show(body, "train");

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(tabs, BorderLayout.WEST);
        card.add(top, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent trainSearch() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(Theme.SP4, 0, 0, 0));

        fromField = new AutoCompleteField(20, StationCatalog::allSuggestions, txt -> doSearch());
        toField = new AutoCompleteField(20, StationCatalog::allSuggestions, txt -> doSearch());
        fromField.setToolTipText("Type a station code or name");
        toField.setToolTipText("Type a station code or name");

        SecondaryButton swapBtn = new SecondaryButton("\u2194");
        swapBtn.setToolTipText("Swap origin and destination");
        swapBtn.setPreferredSize(new Dimension(46, 34));
        swapBtn.setFont(Theme.BODY.deriveFont(Font.BOLD, 16f));
        swapBtn.addActionListener(e -> {
            String tmp = fromField.getText();
            fromField.setText(toField.getText());
            toField.setText(tmp);
        });

        classCombo = new JComboBox<>(ReservationService.CLASS_TYPES);
        classCombo.setFont(Theme.BODY);
        classCombo.setBackground(Theme.SURFACE);
        classCombo.setForeground(Theme.TEXT);
        classCombo.setBorder(BorderFactory.createCompoundBorder(
                new com.railreserve.ui.components.RoundedBorder(Theme.RADIUS_SM, Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(0, Theme.SP4, 0, 0)));
        dateSpinner = new JSpinner(new SpinnerDateModel());
        ((SpinnerDateModel) dateSpinner.getModel()).setStart(startOfToday(0));
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd MMM yyyy"));
        dateSpinner.setBorder(BorderFactory.createCompoundBorder(
                new com.railreserve.ui.components.RoundedBorder(Theme.RADIUS_SM, Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(0, Theme.SP4, 0, 0)));
        dateSpinner.setBackground(Theme.SURFACE);
        dateSpinner.setOpaque(true);
        if (dateSpinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            editor.getTextField().setBackground(Theme.SURFACE);
            editor.getTextField().setForeground(Theme.TEXT);
            editor.getTextField().setBorder(BorderFactory.createEmptyBorder(Theme.SP4, Theme.SP8,
                    Theme.SP4, Theme.SP8));
        }

        PrimaryButton searchBtn = new PrimaryButton("Search trains");
        searchBtn.setPreferredSize(new Dimension(180, 48));
        searchBtn.addActionListener(e -> doSearch());

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(controlBox("FROM", fromField), c);

        c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 1;
        c.insets = new Insets(Theme.SP16, Theme.SP8, 0, Theme.SP8);
        c.anchor = GridBagConstraints.SOUTH;
        panel.add(swapBtn, c);

        c = new GridBagConstraints();
        c.gridy = 0;
        c.gridx = 2;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(controlBox("TO", toField), c);

        c = new GridBagConstraints();
        c.gridy = 1;
        c.gridx = 0;
        c.insets = new Insets(Theme.SP16, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(controlBox("JOURNEY DATE", dateSpinner), c);

        c = new GridBagConstraints();
        c.gridy = 1;
        c.gridx = 1;
        c.gridwidth = 2;
        c.insets = new Insets(Theme.SP16, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        JPanel bottomRow = new JPanel(new GridBagLayout());
        bottomRow.setOpaque(false);
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = 0;
        bc.gridx = 0;
        bc.weightx = 1.0;
        bc.fill = GridBagConstraints.HORIZONTAL;
        bottomRow.add(controlBox("CLASS", classCombo), bc);
        bc.gridx = 1;
        bc.weightx = 0;
        bc.insets = new Insets(Theme.SP16, Theme.SP8, 0, 0);
        bottomRow.add(searchBtn, bc);
        panel.add(bottomRow, c);

        JLabel hint = new JLabel("Not logged in? Search first — we will ask you to login before the booking is saved.");
        hint.setFont(Theme.SMALL);
        hint.setForeground(Theme.MUTED);
        c = new GridBagConstraints();
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 3;
        c.insets = new Insets(Theme.SP16, 0, 0, 0);
        panel.add(hint, c);
        return panel;
    }

    private JComponent pnrSearch() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(Theme.SP4, 0, 0, 0));

        pnrField = new JTextField(12);
        pnrField.setFont(Theme.H3);
        pnrField.setForeground(Theme.TEXT);
        pnrField.setCaretColor(Theme.BRAND);
        pnrField.setBackground(Theme.SURFACE);
        pnrField.setBorder(new javax.swing.border.CompoundBorder(
                new com.railreserve.ui.components.RoundedBorder(Theme.RADIUS_SM, Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(Theme.SP8, Theme.SP12, Theme.SP8, Theme.SP12)));
        pnrField.setPreferredSize(new Dimension(320, 44));

        PrimaryButton checkBtn = new PrimaryButton("Check status");
        checkBtn.setPreferredSize(new Dimension(160, 44));
        checkBtn.addActionListener(e -> checkPnr(checkBtn));
        pnrField.addActionListener(e -> checkPnr(checkBtn));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SP12, Theme.SP8));
        row.setOpaque(false);
        row.add(controlBox("10-DIGIT PNR", pnrField));
        row.add(checkBtn);

        pnrHint = new JLabel("No login needed — PNR status is public in this demo.");
        pnrHint.setFont(Theme.SMALL);
        pnrHint.setForeground(Theme.MUTED);

        pnrResultSlot = new JPanel();
        pnrResultSlot.setOpaque(false);
        pnrResultSlot.setLayout(new BoxLayout(pnrResultSlot, BoxLayout.Y_AXIS));
        pnrResultSlot.setAlignmentX(LEFT_ALIGNMENT);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        panel.add(row, c);
        c.gridy = 1;
        c.insets = new Insets(Theme.SP8, 0, 0, 0);
        panel.add(pnrHint, c);
        c.gridy = 2;
        c.insets = new Insets(Theme.SP12, 0, 0, 0);
        panel.add(pnrResultSlot, c);
        return panel;
    }

    private static Date startOfToday(int daysFromToday) {
        return Date.from(LocalDate.now().plusDays(daysFromToday)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /** Caption above a control: label on top, control below, left-aligned. */
    private JComponent controlBox(String caption, JComponent control) {
        JPanel box = new JPanel(new BorderLayout(0, Theme.SP4));
        box.setOpaque(false);
        JLabel cap = new JLabel(caption);
        cap.setFont(Theme.SMALL.deriveFont(Font.BOLD, 10.5f));
        cap.setForeground(Theme.MUTED);
        box.add(cap, BorderLayout.NORTH);
        control.setPreferredSize(new Dimension(
                Math.max(control.getPreferredSize().width, 150),
                Math.max(control.getPreferredSize().height, 44)));
        box.add(control, BorderLayout.CENTER);
        return box;
    }

    private void doSearch() {
        String from = fromField.getText().strip();
        String to = toField.getText().strip();
        fromField.setErrorState(false);
        toField.setErrorState(false);
        if (from.isEmpty()) {
            fromField.setErrorState(true);
            fromField.requestFocusInWindow();
            return;
        }
        if (to.isEmpty()) {
            toField.setErrorState(true);
            toField.requestFocusInWindow();
            return;
        }
        if (from.equalsIgnoreCase(to)) {
            toField.setErrorState(true);
            toField.requestFocusInWindow();
            return;
        }
        LocalDate date = ((Date) dateSpinner.getValue())
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        onSearch.accept(new SearchIntent(from, to, date, String.valueOf(classCombo.getSelectedItem())));
    }

    // ---------- trust strip ----------

    private JComponent trustStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 4, Theme.SP16, 0));
        strip.setOpaque(true);
        strip.setBackground(Theme.SURFACE);
        strip.setAlignmentX(LEFT_ALIGNMENT);
        strip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(Theme.SP20, Theme.SP48, Theme.SP20, Theme.SP48)));
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        strip.add(trustItem(Icons.ticket(20), "10-digit PNR", "unique, never reused"));
        strip.add(trustItem(Icons.bolt(20), "Instant confirmation", "no waiting list"));
        strip.add(trustItem(Icons.cancel(20), "One-click cancel", "status flips live"));
        strip.add(trustItem(Icons.shield(20), "PBKDF2 hashing", "never stored in plain text"));
        return strip;
    }

    private JComponent trustItem(Icon icon, String title, String caption) {
        JPanel item = new JPanel(new BorderLayout(Theme.SP8, 0));
        item.setOpaque(false);
        JLabel glyph = new JLabel(icon);
        item.add(glyph, BorderLayout.WEST);

        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(Theme.BODY.deriveFont(Font.BOLD, 12.5f));
        t.setForeground(Theme.TEXT);
        JLabel s = new JLabel(caption);
        s.setFont(Theme.SMALL.deriveFont(11f));
        s.setForeground(Theme.MUTED);
        text.add(t, BorderLayout.NORTH);
        text.add(s, BorderLayout.CENTER);
        item.add(text, BorderLayout.CENTER);
        return item;
    }

    // ---------- shared section scaffolding ----------

    private JComponent sectionHeading(String title, String sub) {
        JPanel head = new JPanel(new BorderLayout(0, Theme.SP4));
        head.setOpaque(false);
        head.setAlignmentX(LEFT_ALIGNMENT);
        JLabel t = new JLabel(title);
        t.setFont(Theme.H1);
        t.setForeground(Theme.TEXT);
        head.add(t, BorderLayout.NORTH);
        if (sub != null) {
            JLabel s = new JLabel(sub);
            s.setFont(Theme.BODY);
            s.setForeground(Theme.MUTED);
            head.add(s, BorderLayout.CENTER);
        }
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.SP16, 0));
        return head;
    }

    /** Card with a soft orange icon badge, a bold title and a muted paragraph. */
    private Card iconCard(Icon vectorIcon, String title, String body) {
        Card card = new Card(new BorderLayout(0, Theme.SP12));
        card.setPreferredSize(new Dimension(220, 168));
        card.setMaximumSize(new Dimension(320, 190));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel top = new JPanel(new BorderLayout(0, Theme.SP12));
        top.setOpaque(false);
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(new IconBadge(vectorIcon));
        top.add(badgeRow, BorderLayout.NORTH);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.H3);
        titleLbl.setForeground(Theme.TEXT);
        top.add(titleLbl, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(bodyText(body), BorderLayout.CENTER);
        return card;
    }

    /** Step card keeps its number in a soft badge. */
    private Card stepCard(String number, String title, String body) {
        Card card = new Card(new BorderLayout(0, Theme.SP12));
        card.setPreferredSize(new Dimension(220, 150));
        card.setMaximumSize(new Dimension(320, 170));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel top = new JPanel(new BorderLayout(0, Theme.SP12));
        top.setOpaque(false);
        JLabel badge = new JLabel(number, SwingConstants.CENTER);
        badge.setFont(Theme.H2);
        badge.setForeground(Theme.BRAND);
        Badge badgePanel = new Badge(badge);
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(badgePanel);
        top.add(badgeRow, BorderLayout.NORTH);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.H3);
        titleLbl.setForeground(Theme.TEXT);
        top.add(titleLbl, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(bodyText(body), BorderLayout.CENTER);
        return card;
    }

    private JTextArea bodyText(String body) {
        JTextArea bodyLbl = new JTextArea(body);
        bodyLbl.setFont(Theme.SMALL);
        bodyLbl.setForeground(Theme.MUTED);
        bodyLbl.setEditable(false);
        bodyLbl.setLineWrap(true);
        bodyLbl.setWrapStyleWord(true);
        bodyLbl.setOpaque(false);
        return bodyLbl;
    }

    private JComponent spacer(int height) {
        JComponent gap = (JComponent) Box.createVerticalStrut(height);
        gap.setOpaque(false);
        return gap;
    }

    /** Section shell: heading on top, content row underneath, side margins. */
    private JPanel wrapSection(JComponent heading) {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, Theme.SP48, 0, Theme.SP48));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        wrap.add(heading, c);
        return wrap;
    }

    /** Adds a section's content row underneath its heading. */
    private void addRow(JPanel wrap, JComponent row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        wrap.add(row, c);
    }

    // ---------- why cards / how it works ----------

    private JComponent whyCards() {
        JPanel wrap = wrapSection(sectionHeading("Why book with RailReserve",
                "Every claim below is true inside this application."));
        JPanel row = new JPanel(new GridLayout(1, 4, Theme.SP16, Theme.SP16));
        row.setOpaque(false);
        row.add(iconCard(Icons.search(24), "Simple booking",
                "One form: stations, train, class and date. Confirm in two clicks."));
        row.add(iconCard(Icons.ticket(24), "Unique PNR",
                "Every confirmed booking gets a 10-digit PNR that is never reused."));
        row.add(iconCard(Icons.cancel(24), "Easy cancellation",
                "Open My Bookings, pick the ticket, confirm — status flips instantly."));
        row.add(iconCard(Icons.lock(24), "Secure login",
                "Passwords are hashed with PBKDF2 and never stored or logged in plain text."));
        addRow(wrap, row);
        return wrap;
    }

    private JComponent howItWorks() {
        JPanel wrap = wrapSection(sectionHeading("How it works", "Three steps from search to a booked ticket."));
        JPanel row = new JPanel(new GridLayout(1, 3, Theme.SP16, Theme.SP16));
        row.setOpaque(false);
        row.add(stepCard("1", "Search", "Pick origin, destination and date — the fields suggest stations while you type."));
        row.add(stepCard("2", "Book", "Fill passenger and class details, review, and confirm."));
        row.add(stepCard("3", "Get your PNR", "A 10-digit PNR is issued immediately; check its status any time."));
        addRow(wrap, row);
        return wrap;
    }

    // ---------- PNR lookup ----------

    private void checkPnr(PrimaryButton button) {
        String pnr = pnrField.getText().strip();
        if (!pnr.matches("\\d{10}")) {
            pnrHint.setText("A PNR is exactly 10 digits — double-check and try again.");
            pnrHint.setForeground(Theme.ERROR);
            pnrField.requestFocusInWindow();
            return;
        }
        pnrHint.setText("Looking up your booking…");
        pnrHint.setForeground(Theme.MUTED);
        pnrField.setEnabled(false);
        button.setEnabled(false);
        LoadingSpinner spinner = new LoadingSpinner(24);
        showPnrResult(spinner);

        new SwingWorker<Optional<Reservation>, Void>() {
            @Override
            protected Optional<Reservation> doInBackground() {
                try {
                    return reservationService.findByPnr(pnr);
                } catch (SQLException ex) {
                    return Optional.empty();
                }
            }

            @Override
            protected void done() {
                pnrField.setEnabled(true);
                button.setEnabled(true);
                try {
                    Optional<Reservation> result = get();
                    if (result.isPresent()) {
                        pnrHint.setText("Here is the latest status for this booking.");
                        pnrHint.setForeground(Theme.SUCCESS);
                        showPnrResult(new TicketCard(result.get()));
                    } else {
                        pnrHint.setText("No booking with that PNR exists in this demo database.");
                        pnrHint.setForeground(Theme.ERROR);
                        showPnrResult(new JLabel(" "));
                    }
                } catch (Exception ex) {
                    pnrHint.setText("Something went wrong while checking — please try again in a moment.");
                    pnrHint.setForeground(Theme.ERROR);
                    showPnrResult(new JLabel(" "));
                }
            }
        }.execute();
    }

    private void showPnrResult(JComponent content) {
        pnrResultSlot.removeAll();
        content.setAlignmentX(LEFT_ALIGNMENT);
        pnrResultSlot.add(content);
        pnrResultSlot.revalidate();
        pnrResultSlot.repaint();
    }

    // ---------- popular routes ----------

    private JComponent popularRoutes() {
        JPanel wrap = wrapSection(sectionHeading("Popular routes",
                "Click a route to pre-fill the search above."));
        JPanel grid = new JPanel(new GridLayout(2, 3, Theme.SP16, Theme.SP16));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        for (StationCatalog.PopularRoute route : StationCatalog.popularRoutes()) {
            grid.add(routeCard(route));
        }
        addRow(wrap, grid);
        return wrap;
    }

    private JComponent routeCard(StationCatalog.PopularRoute route) {
        Card card = new Card(new BorderLayout(0, Theme.SP8));
        card.setCursor(Theme.HAND);
        card.setToolTipText("Pre-fill search: " + route.fromCode() + " → " + route.toCode());

        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SP8, 0));
        line.setOpaque(false);
        JLabel from = new JLabel(route.fromCode());
        from.setFont(Theme.H3.deriveFont(Font.BOLD, 16f));
        from.setForeground(Theme.TEXT);
        JLabel arrow = new JLabel("\u2192");
        arrow.setFont(Theme.BODY);
        arrow.setForeground(Theme.BRAND);
        JLabel to = new JLabel(route.toCode());
        to.setFont(Theme.H3.deriveFont(Font.BOLD, 16f));
        to.setForeground(Theme.TEXT);
        line.add(from);
        line.add(arrow);
        line.add(to);

        JLabel names = new JLabel(route.fromName() + "  ·  " + route.toName());
        names.setFont(Theme.SMALL);
        names.setForeground(Theme.MUTED);
        JLabel trainLine = new JLabel(route.trainNumber() + " · " + route.trainName());
        trainLine.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11.5f));
        trainLine.setForeground(Theme.BRAND);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        for (JLabel l : List.of(names, trainLine)) {
            l.setAlignmentX(LEFT_ALIGNMENT);
            text.add(l);
        }

        JLabel info = new JLabel(route.info());
        info.setFont(Theme.SMALL);
        info.setForeground(Theme.MUTED);
        JLabel search = new JLabel("Search  \u2192");
        search.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11.5f));
        search.setForeground(Theme.BRAND);
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setBorder(BorderFactory.createEmptyBorder(Theme.SP8, 0, 0, 0));
        foot.add(info, BorderLayout.WEST);
        foot.add(search, BorderLayout.EAST);

        card.add(line, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);
        card.add(foot, BorderLayout.SOUTH);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                fromField.setText("(" + route.fromCode() + ") " + route.fromName());
                toField.setText("(" + route.toCode() + ") " + route.toName());
                fromField.requestFocusInWindow();
            }
        });
        return card;
    }

    // ---------- FAQ ----------

    private record FaqEntry(String question, String answer) {
    }

    private JComponent faq() {
        JPanel wrap = wrapSection(sectionHeading("Frequently asked questions",
                "Everything usually asked before the first booking."));
        List<FaqEntry> entries = List.of(
                new FaqEntry("How do I book a ticket?",
                        "Login, open Book Ticket, enter a train number (the train name fills in "
                                + "automatically), pick class and date, type the passenger name, then confirm."),
                new FaqEntry("How do I cancel a ticket?",
                        "Go to My Bookings (or Cancel Ticket), find the CONFIRMED booking by PNR, press "
                                + "Cancel and confirm. The status changes to CANCELLED immediately."),
                new FaqEntry("What is a PNR?",
                        "PNR is the 10-digit unique code issued when your booking is confirmed. Use it "
                                + "under PNR Status any time to see the ticket's current state."),
                new FaqEntry("Is this real IRCTC?",
                        "No. RailReserve is an academic demo project — it is not affiliated with IRCTC or "
                                + "Indian Railways, and no real tickets are issued."));
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setAlignmentX(LEFT_ALIGNMENT);
        for (FaqEntry entry : entries) {
            list.add(new FaqItem(entry));
            list.add(Box.createVerticalStrut(Theme.SP8));
        }
        list.setAlignmentX(LEFT_ALIGNMENT);
        addRow(wrap, list);
        return wrap;
    }

    /** One collapsible question styled like ixigo's FAQ rows. */
    private final class FaqItem extends Card {
        private final JTextArea answer;

        FaqItem(FaqEntry entry) {
            super(new BorderLayout(Theme.SP12, Theme.SP8));
            setAlignmentX(LEFT_ALIGNMENT);
            this.answer = new JTextArea(entry.answer());
            answer.setFont(Theme.BODY);
            answer.setForeground(Theme.MUTED);
            answer.setEditable(false);
            answer.setLineWrap(true);
            answer.setWrapStyleWord(true);
            answer.setOpaque(false);
            answer.setVisible(false);

            SecondaryButton toggle = SecondaryButton.link("+   " + entry.question());
            toggle.setFont(Theme.BODY.deriveFont(Font.BOLD, 13.5f));
            toggle.setHorizontalAlignment(SwingConstants.LEFT);
            toggle.addActionListener(e -> {
                boolean open = answer.isVisible();
                answer.setVisible(!open);
                toggle.setText((open ? "+   " : "\u2013   ") + entry.question());
                revalidate();
                repaint();
            });
            add(toggle, BorderLayout.NORTH);
            add(answer, BorderLayout.CENTER);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    // ---------- footer ----------

    private JComponent footer() {
        JPanel root = new JPanel(new BorderLayout(0, Theme.SP16));
        root.setBackground(Theme.SURFACE);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(Theme.SP32, Theme.SP48, Theme.SP24, Theme.SP48)));
        root.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        JPanel columns = new JPanel(new GridLayout(1, 3, Theme.SP32, 0));
        columns.setOpaque(false);
        columns.add(footerColumn("RailReserve",
                "An academic OASIS Infobyte task: Java 17 + Swing on top of PostgreSQL."));
        columns.add(footerColumn("Product",
                "Train search, booking, PNR status and cancellation — all in one window."));
        columns.add(footerColumn("Good to know",
                "Demo data only. No real tickets, no IRCTC affiliation, no payments."));
        root.add(columns, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SP16, 0));
        links.setOpaque(false);
        for (String target : List.of("About", "Help", "Privacy")) {
            SecondaryButton link = SecondaryButton.link(target);
            link.addActionListener(e -> onNavLink.accept(target));
            links.add(link);
        }
        bottom.add(links, BorderLayout.WEST);
        JLabel note = new JLabel("© 2026 RailReserve demo — not affiliated with IRCTC or Indian Railways.");
        note.setForeground(Theme.MUTED);
        note.setFont(Theme.SMALL);
        bottom.add(note, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private JComponent footerColumn(String title, String body) {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(Theme.H3);
        t.setForeground(Theme.TEXT);
        t.setAlignmentX(LEFT_ALIGNMENT);
        JLabel s = new JLabel("<html><body style='width:260px'>" + body + "</body></html>");
        s.setFont(Theme.SMALL);
        s.setForeground(Theme.MUTED);
        s.setAlignmentX(LEFT_ALIGNMENT);
        column.add(t);
        column.add(Box.createVerticalStrut(Theme.SP8));
        column.add(s);
        return column;
    }

    // ---------- painted pieces ----------

    /** Hero band: brand tint fading into the page background, with soft blobs. */
    private static final class HeroPanel extends JPanel {
        HeroPanel() {
            setOpaque(false);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new LinearGradientPaint(
                    0, 0, 0, Math.max(1, getHeight()),
                    new float[]{0f, 1f},
                    new Color[]{Theme.BRAND_SOFT, Theme.BACKGROUND}));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(Theme.BRAND.getRed(), Theme.BRAND.getGreen(),
                    Theme.BRAND.getBlue(), Theme.isDark() ? 22 : 12));
            int blob = Math.max(220, getWidth() / 3);
            g2.fillOval(getWidth() - blob / 2, -blob / 2, blob, blob);
            g2.fillOval(-blob / 3, getHeight() - blob / 2, blob / 2, blob / 2);
            g2.dispose();
        }
    }

    /** Page body that never grows wider than the viewport (no side clipping). */
    private static final class SectionsPanel extends JPanel implements javax.swing.Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
            return Math.max(80, visibleRect.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Small round white pill used behind the eyebrow label. */
    private static final class RoundedTint extends JPanel {
        RoundedTint(JComponent inner) {
            super(new BorderLayout());
            setOpaque(false);
            add(inner, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.SURFACE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 44px soft-orange circle behind a benefit icon. */
    private static final class IconBadge extends JComponent {
        private final Icon icon;

        IconBadge(Icon icon) {
            this.icon = icon;
            setPreferredSize(new Dimension(44, 44));
            setMaximumSize(new Dimension(44, 44));
            setAlignmentX(LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(44, 44);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.BRAND_SOFT);
            g2.fillRoundRect(0, 0, 44, 44, Theme.RADIUS, Theme.RADIUS);
            g2.dispose();
            icon.paintIcon(this, g, 10, 10);
        }
    }

    /** Soft-orange circle behind a step number. */
    private static final class Badge extends JPanel {
        Badge(JLabel number) {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(44, 44));
            setMaximumSize(new Dimension(44, 44));
            add(number, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.BRAND_SOFT);
            g2.fillRoundRect(0, 0, 44, 44, Theme.RADIUS, Theme.RADIUS);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
