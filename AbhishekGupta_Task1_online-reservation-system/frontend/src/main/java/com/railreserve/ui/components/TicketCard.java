package com.railreserve.ui.components;

import com.railreserve.model.Reservation;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Reusable "looks like a ticket" panel for a Reservation: PNR + status header,
 * dashed tear line, route, train, passenger, class and journey date in a tidy
 * two-column grid. An optional trailing action (e.g. "Cancel this ticket")
 * attaches to the bottom.
 */
public class TicketCard extends Card {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");

    public TicketCard(Reservation reservation) {
        this(reservation, null);
    }

    public TicketCard(Reservation reservation, Consumer<Reservation> trailingAction) {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(Theme.SP20, Theme.SP24, Theme.SP16, Theme.SP24));
        showTicket(reservation, trailingAction);
    }

    /** Builds or rebuilds the ticket contents. */
    public void showTicket(Reservation reservation, Consumer<Reservation> trailingAction) {
        removeAll();
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = -1;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;

        // header row: PNR caption + value, status pill on the right
        JPanel pnrBox = new JPanel(new BorderLayout(0, 2));
        pnrBox.setOpaque(false);
        JLabel caption = new JLabel("PNR NUMBER");
        caption.setFont(Theme.SMALL.deriveFont(Font.BOLD, 10.5f));
        caption.setForeground(Theme.MUTED);
        JLabel pnr = new JLabel(reservation.getPnr());
        pnr.setFont(Theme.H2.deriveFont(22f));
        pnr.setForeground(Theme.BRAND);
        pnrBox.add(caption, BorderLayout.NORTH);
        pnrBox.add(pnr, BorderLayout.CENTER);

        JPanel head = new JPanel(new BorderLayout(Theme.SP12, 0));
        head.setOpaque(false);
        head.add(pnrBox, BorderLayout.WEST);
        head.add(new StatusChip(reservation.getStatus()), BorderLayout.EAST);

        add(head, next(gc, Theme.SP4));

        // dashed tear line
        add(new DashedRule(), next(gc, Theme.SP12));

        // two-column detail grid
        JPanel grid = new JPanel(new GridLayout(3, 2, Theme.SP16, Theme.SP12));
        grid.setOpaque(false);
        grid.add(field("Route", reservation.getSourceStation() + "  →  "
                + reservation.getDestinationStation()));
        grid.add(field("Train", reservation.getTrainNumber() + " · " + reservation.getTrainName()));
        grid.add(field("Passenger", reservation.getPassengerName()));
        grid.add(field("Class", reservation.getClassType()));
        grid.add(field("Journey date", reservation.getJourneyDate() == null ? "—"
                : reservation.getJourneyDate().format(DATE_FMT)));
        grid.add(field("Booked on", reservation.getCreatedAt() == null ? "—"
                : reservation.getCreatedAt().toLocalDate().format(DATE_FMT)));

        add(grid, next(gc, Theme.SP8));

        if (trailingAction != null) {
            SecondaryButton action = new SecondaryButton(
                    reservation.isCancelled() ? "Already cancelled" : "Cancel this ticket");
            action.setEnabled(!reservation.isCancelled());
            action.addActionListener(e -> trailingAction.accept(reservation));
            GridBagConstraints actionCell = next(gc, Theme.SP16);
            actionCell.fill = GridBagConstraints.NONE;
            actionCell.anchor = GridBagConstraints.WEST;
            add(action, actionCell);
        }
    }

    /** Advances to the next row and applies the top inset. */
    private static GridBagConstraints next(GridBagConstraints gc, int topInset) {
        gc.gridy++;
        gc.insets = new Insets(topInset, 0, 0, 0);
        GridBagConstraints copy = (GridBagConstraints) gc.clone();
        gc.insets = new Insets(0, 0, 0, 0);
        return copy;
    }

    /** Caption over value, left aligned. */
    private JComponent field(String captionText, String value) {
        JPanel line = new JPanel(new BorderLayout(0, 2));
        line.setOpaque(false);
        JLabel caption = new JLabel(captionText.toUpperCase());
        caption.setFont(Theme.SMALL.deriveFont(Font.BOLD, 10.5f));
        caption.setForeground(Theme.MUTED);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(Theme.BODY.deriveFont(Font.BOLD, 13.5f));
        valueLabel.setForeground(Theme.TEXT);
        line.add(caption, BorderLayout.NORTH);
        line.add(valueLabel, BorderLayout.CENTER);
        return line;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /** Hairline dashed separator, like the tear line on an e-ticket. */
    private static final class DashedRule extends JComponent {
        DashedRule() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.BORDER);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{5f, 5f}, 0f));
            g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(1, 1);
        }
    }
}
