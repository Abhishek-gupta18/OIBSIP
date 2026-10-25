package com.railreserve.ui.dashboard;

import com.railreserve.model.Reservation;
import com.railreserve.service.ReservationService;
import com.railreserve.ui.UserSession;
import com.railreserve.ui.components.Card;
import com.railreserve.ui.components.EmptyState;
import com.railreserve.ui.components.Icons;
import com.railreserve.ui.components.PrimaryButton;
import com.railreserve.ui.components.SecondaryButton;
import com.railreserve.ui.components.StatusChip;
import com.railreserve.ui.components.TicketCard;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Overview section: greeting with the user's name, stat cards (total /
 * upcoming / cancelled), next CONFIRMED journey, five most recent bookings and
 * quick-action buttons. One SwingWorker load; friendly errors + empty state.
 */
public class OverviewPanel extends JPanel {

    private final DashboardHost host;
    private final int userId;
    private final ReservationService service = new ReservationService();
    private final JPanel statRow = new JPanel(new GridLayout(1, 3, Theme.SP16, Theme.SP16));
    private final JPanel nextJourneySlot = new JPanel(new GridBagLayout());
    private final JPanel recentSlot = new JPanel();
    private final JLabel errorNote = new JLabel(" ");

    public OverviewPanel(DashboardHost host, int userId) {
        this.host = host;
        this.userId = userId;
        setLayout(new BorderLayout());

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP32, Theme.SP24, Theme.SP32));

        GridBagConstraints c = cell(0, 0, GridBagConstraints.HORIZONTAL);
        body.add(heading(), c);
        body.add(statRow, cell(0, 1, GridBagConstraints.HORIZONTAL));

        JLabel nextTitle = sectionTitle("Next journey");
        body.add(nextTitle, cell(0, 2, GridBagConstraints.HORIZONTAL));
        nextJourneySlot.setOpaque(false);
        nextJourneySlot.setLayout(new GridBagLayout());
        nextJourneySlot.add(new LoadingPlaceholder("Loading your next journey…"), new GridBagConstraints());
        body.add(nextJourneySlot, cell(0, 3, GridBagConstraints.HORIZONTAL));

        JLabel recentTitle = sectionTitle("Recent bookings");
        body.add(recentTitle, cell(0, 4, GridBagConstraints.HORIZONTAL));
        recentSlot.setOpaque(false);
        recentSlot.setLayout(new BoxLayout(recentSlot, BoxLayout.Y_AXIS));
        recentSlot.add(new LoadingPlaceholder("Loading recent bookings…"));
        body.add(recentSlot, cell(0, 5, GridBagConstraints.HORIZONTAL));

        errorNote.setForeground(Theme.ERROR);
        errorNote.setFont(Theme.SMALL);
        body.add(errorNote, cell(0, 6, GridBagConstraints.HORIZONTAL));

        GridBagConstraints filler = cell(0, 7, GridBagConstraints.BOTH);
        filler.weighty = 1.0;
        JPanel pad = new JPanel();
        pad.setOpaque(false);
        body.add(pad, filler);

        JScrollPane scroll = new JScrollPane(body,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        reload();
    }

    /** GridBag cell helper: one column, fixed vertical stacking. */
    private GridBagConstraints cell(int x, int y, int fill) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = 1.0;
        c.fill = fill;
        c.anchor = GridBagConstraints.NORTHWEST;
        if (y > 0) {
            c.insets = new Insets(y == 2 || y == 4 ? Theme.SP24 : Theme.SP8, 0, 0, 0);
        }
        return c;
    }

    private JComponent heading() {
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel hi = new JLabel("Hi, " + UserSession.displayName());
        hi.setFont(Theme.H1);
        hi.setForeground(Theme.TEXT);
        JLabel quick = new JLabel("Here is where your travels stand today.");
        quick.setFont(Theme.BODY);
        quick.setForeground(Theme.MUTED);
        PrimaryButton bookNow = new PrimaryButton("Book a ticket");
        bookNow.setPreferredSize(new Dimension(150, 40));
        bookNow.addActionListener(e -> host.showSection("book"));
        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(bookNow);
        head.add(textColumn(hi, quick), BorderLayout.CENTER);
        head.add(right, BorderLayout.EAST);
        return head;
    }

    private JComponent textColumn(JLabel top, JLabel bottom) {
        JPanel col = new JPanel();
        col.setLayout(new javax.swing.BoxLayout(col, javax.swing.BoxLayout.Y_AXIS));
        col.setOpaque(false);
        top.setAlignmentX(0f);
        bottom.setAlignmentX(0f);
        col.add(top);
        col.add(bottom);
        return col;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.H2);
        label.setForeground(Theme.TEXT);
        label.setAlignmentX(0f);
        return label;
    }

    /** Stat card: big orange number over a quiet caption. */

    /** Loads the whole overview from one history call (SwingWorker). */
    private void reload() {
        SwingWorker<List<Reservation>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Reservation> doInBackground() throws Exception {
                return service.historyFor(userId);
            }

            @Override
            protected void done() {
                try {
                    render(get());
                } catch (Exception ex) {
                    statRow.removeAll();
                    recentSlot.removeAll();
                    nextJourneySlot.removeAll();
                    errorNote.setText("Could not load your bookings — check the connection and reopen Overview.");
                    revalidate();
                    repaint();
                }
            }
        };
        worker.execute();
    }

    private void render(List<Reservation> history) {
        long total = history.size();
        long cancelled = history.stream().filter(Reservation::isCancelled).count();
        long upcoming = history.stream()
                .filter(r -> !r.isCancelled() && r.getJourneyDate() != null
                        && !r.getJourneyDate().isBefore(LocalDate.now()))
                .count();

        statRow.removeAll();
        statRow.add(statCard(String.valueOf(total), "Total bookings"));
        statRow.add(statCard(String.valueOf(upcoming), "Upcoming journeys"));
        statRow.add(statCard(String.valueOf(cancelled), "Cancelled"));
        statRow.revalidate();
        statRow.repaint();

        nextJourneySlot.removeAll();
        Optional<Reservation> next = history.stream()
                .filter(r -> !r.isCancelled() && r.getJourneyDate() != null
                        && !r.getJourneyDate().isBefore(LocalDate.now()))
                .min(Comparator.comparing(Reservation::getJourneyDate));
        if (next.isPresent()) {
            GridBagConstraints wide = new GridBagConstraints();
            wide.fill = GridBagConstraints.HORIZONTAL;
            wide.weightx = 1.0;
            nextJourneySlot.add(new TicketCard(next.get()), wide);
        } else {
            nextJourneySlot.add(new EmptyState(Icons.train(40), "No upcoming journeys",
                    "Book a train and it will show up here with its PNR."));
        }
        nextJourneySlot.revalidate();
        nextJourneySlot.repaint();

        recentSlot.removeAll();
        List<Reservation> recent = history.stream()
                .sorted(Comparator.comparing(
                        (Reservation r) -> r.getCreatedAt() == null ? java.time.LocalDateTime.MIN : r.getCreatedAt())
                        .reversed())
                .limit(5)
                .toList();
        if (recent.isEmpty()) {
            SecondaryButton action = new SecondaryButton("Book your first ticket");
            action.addActionListener(e -> host.showSection("book"));
            recentSlot.add(new EmptyState(Icons.ticket(40), "Nothing booked yet",
                    "When you book, your latest PNRs appear here.", action));
        } else {
            for (Reservation r : recent) {
                recentSlot.add(bookingRow(r));
                recentSlot.add(javax.swing.Box.createVerticalStrut(Theme.SP8));
            }
        }
        recentSlot.revalidate();
        recentSlot.repaint();
        errorNote.setText(" ");
    }

    private JComponent bookingRow(Reservation r) {
        Card row = new Card(new BorderLayout(Theme.SP12, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        Card c1 = row;

        JLabel pnr = new JLabel(r.getPnr());
        pnr.setFont(Theme.H3);
        pnr.setForeground(Theme.BRAND);
        JLabel route = new JLabel(r.getSourceStation() + " → " + r.getDestinationStation()
                + "  ·  " + r.getTrainNumber() + " " + r.getTrainName());
        route.setFont(Theme.SMALL);
        route.setForeground(Theme.MUTED);

        JPanel left = new JPanel(new java.awt.GridBagLayout());
        left.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        left.add(pnr, gc);
        gc.gridy = 1;
        left.add(route, gc);
        c1.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(Theme.SP12, 0));
        right.setOpaque(false);
        JLabel date = new JLabel(r.getJourneyDate() == null ? "—"
                : r.getJourneyDate().format(TicketCard.DATE_FMT));
        date.setFont(Theme.SMALL);
        date.setForeground(Theme.MUTED);
        right.add(date, BorderLayout.CENTER);
        right.add(new StatusChip(r.getStatus()), BorderLayout.EAST);
        c1.add(right, BorderLayout.EAST);
        return c1;
    }

    private JComponent statCard(String big, String caption) {
        Card card = new Card(new BorderLayout(0, Theme.SP4));
        card.setPreferredSize(new Dimension(200, 122));
        JLabel number = new JLabel(big);
        number.setFont(Theme.H1.deriveFont(Font.BOLD, 32f));
        number.setForeground(Theme.BRAND);
        JLabel cap = new JLabel(caption.toUpperCase());
        cap.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11f));
        cap.setForeground(Theme.MUTED);
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(number, BorderLayout.CENTER);
        JPanel capRow = new JPanel(new java.awt.GridBagLayout());
        capRow.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        capRow.add(cap, gc);
        body.add(capRow, BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }
}

/** Small inline loading note used while data loads. */
final class LoadingPlaceholder extends JLabel {
    LoadingPlaceholder(String text) {
        super(text);
        setFont(Theme.BODY);
        setForeground(Theme.MUTED);
        setOpaque(false);
    }
}
